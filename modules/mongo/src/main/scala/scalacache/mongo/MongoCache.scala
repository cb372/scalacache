/*
 * Copyright 2021 scalacache
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package scalacache.mongo

import cats.effect.Async
import cats.effect.Resource
import cats.syntax.all._
import org.mongodb.scala._
import org.mongodb.scala.bson.BsonDateTime
import org.mongodb.scala.bson.BsonNull
import org.mongodb.scala.model._
import scalacache.AbstractCache
import scalacache.logging.Logger
import scalacache.serialization.bson.BsonCodec
import scalacache.serialization.bson.BsonEncoder

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

class MongoCache[F[_]: Async, K, V](client: MongoClient, databaseName: String, collectionName: String)(implicit
    val keyEncoder: BsonEncoder[K],
    val valueCodec: BsonCodec[V]
) extends AbstractCache[F, K, V] {

  protected def F: Async[F] = Async[F]

  protected final val logger =
    Logger.getLogger[F](getClass.getName)

  private val collection = client
    .getDatabase(databaseName)
    .getCollection(collectionName)

  override protected def doGet(key: K): F[Option[V]] = {
    val findAndDecode = F.delay {
      collection
        .find(Filters.eq("_id", keyEncoder.encode(key)))
        .map { document =>
          valueCodec.decode(document("value"))
        }
        .headOption()
    }

    F.rethrow(
      F.fromFuture(
        findAndDecode
      ).map(_.sequence)
    )
  }

  private def makeCacheEntry(key: K, value: V, maybeTtl: Option[Duration], currentTime: Instant): Document = {
    val expiresAt = maybeTtl
      .map { ttl =>
        val ttlInstant = currentTime.plus(ttl.toMillis, ChronoUnit.MILLIS)
        BsonDateTime(ttlInstant.toEpochMilli)
      }
      .getOrElse {
        BsonNull()
      }

    Document(
      "_id"       -> keyEncoder.encode(key),
      "value"     -> valueCodec.encode(value),
      "expiresAt" -> expiresAt
    )
  }

  override protected def doPut(key: K, value: V, ttl: Option[Duration]): F[Unit] = {
    for {
      currentTime <- F.realTimeInstant

      cacheEntry = makeCacheEntry(key, value, ttl, currentTime)

      upsert = ReplaceOptions().upsert(true)

      keyFilter = Filters.eq("_id", keyEncoder.encode(key))

      encodeAndPut = F.delay {
        collection
          .replaceOne(keyFilter, cacheEntry, upsert)
          .head()
      }

      _ <- F.fromFuture(encodeAndPut)

    } yield ()
  }

  override protected def doRemove(key: K): F[Unit] = {
    F.fromFuture {
      F.delay {
        collection
          .deleteOne(
            Filters.eq("_id", keyEncoder.encode(key))
          )
          .head()
      }
    }.void
  }

  override protected def doRemoveAll: F[Unit] = {
    F.fromFuture {
      F.delay {
        collection
          .deleteMany(Filters.empty())
          .head()
      }
    }.void
  }

  override def close: F[Unit] =
    F.delay(client.close())
}

object MongoCache {
  def apply[F[_], K, V](databaseName: String, collectionName: String)(implicit
      F: Async[F],
      keyEncoder: BsonEncoder[K],
      valueCodec: BsonCodec[V]
  ): F[MongoCache[F, K, V]] = {
    apply("mongodb://localhost:27017", databaseName, collectionName)
  }

  def apply[F[_], K, V](connectionString: String, databaseName: String, collectionName: String)(implicit
      F: Async[F],
      keyEncoder: BsonEncoder[K],
      valueCodec: BsonCodec[V]
  ): F[MongoCache[F, K, V]] = {
    val mongoClient = MongoClient(connectionString)
    apply(mongoClient, databaseName, collectionName)
  }

  def apply[F[_], K, V](mongoClientSettings: MongoClientSettings, databaseName: String, collectionName: String)(implicit
      F: Async[F],
      keyEncoder: BsonEncoder[K],
      valueCodec: BsonCodec[V]
  ): F[MongoCache[F, K, V]] = {
    val mongoClient = MongoClient(mongoClientSettings)
    apply(mongoClient, databaseName, collectionName)
  }

  def apply[F[_], K, V](client: MongoClient, databaseName: String, collectionName: String)(implicit
      F: Async[F],
      keyEncoder: BsonEncoder[K],
      valueCodec: BsonCodec[V]
  ): F[MongoCache[F, K, V]] = {
    val collection = client
      .getDatabase(databaseName)
      .getCollection(collectionName)

    val indexName    = Indexes.ascending("expiresAt")
    val indexOptions = IndexOptions().expireAfter(0, TimeUnit.MILLISECONDS)

    val createIndex = F.delay {
      collection
        .createIndex(indexName, indexOptions)
        .head()
    }

    F.fromFuture(createIndex).as {
      new MongoCache[F, K, V](client, databaseName, collectionName)
    }
  }

  def resource[F[_], K, V](databaseName: String, collectionName: String)(implicit
      F: Async[F],
      keyEncoder: BsonEncoder[K],
      valueCodec: BsonCodec[V]
  ): Resource[F, MongoCache[F, K, V]] = {
    resource("mongodb://localhost:27017", databaseName, collectionName)
  }

  def resource[F[_], K, V](connectionString: String, databaseName: String, collectionName: String)(implicit
      F: Async[F],
      keyEncoder: BsonEncoder[K],
      valueCodec: BsonCodec[V]
  ): Resource[F, MongoCache[F, K, V]] = {
    val mongoClient = MongoClient(connectionString)
    resource(mongoClient, databaseName, collectionName)
  }

  def resource[F[_], K, V](mongoClientSettings: MongoClientSettings, databaseName: String, collectionName: String)(
      implicit
      F: Async[F],
      keyEncoder: BsonEncoder[K],
      valueCodec: BsonCodec[V]
  ): Resource[F, MongoCache[F, K, V]] = {
    val mongoClient = MongoClient(mongoClientSettings)
    resource(mongoClient, databaseName, collectionName)
  }

  private[mongo] def resource[F[_], K, V](client: MongoClient, databaseName: String, collectionName: String)(implicit
      F: Async[F],
      keyEncoder: BsonEncoder[K],
      valueCodec: BsonCodec[V]
  ): Resource[F, MongoCache[F, K, V]] =
    Resource.make(apply[F, K, V](client, databaseName, collectionName))(_.close)
}
