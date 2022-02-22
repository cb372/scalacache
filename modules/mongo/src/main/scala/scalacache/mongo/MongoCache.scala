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
import cats.syntax.all._
import org.mongodb.scala._
import org.mongodb.scala.bson.BsonDateTime
import org.mongodb.scala.bson.BsonNull
import org.mongodb.scala.model._
import scalacache.AbstractCache
import scalacache.logging.Logger
import scalacache.serialization.bson.BsonCodec

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

class MongoCache[F[_]: Async, V](client: MongoClient, databaseName: String, collectionName: String)(implicit
    val codec: BsonCodec[V]
) extends AbstractCache[F, String, V] {

  protected def F: Async[F] = Async[F]

  protected final val logger =
    Logger.getLogger[F](getClass.getName)

  private val collection = client
    .getDatabase(databaseName)
    .getCollection(collectionName)

  override protected def doGet(key: String): F[Option[V]] = {
    val findAndDecode = F.delay {
      collection
        .find(Filters.eq("_id", key))
        .map { document =>
          codec.decode(document("value"))
        }
        .headOption()
    }

    F.rethrow(
      F.fromFuture(
        findAndDecode
      ).map(_.sequence)
    )
  }

  private def makeCacheEntry(key: String, value: V, maybeTtl: Option[Duration], currentTime: Instant): Document = {
    val expiresAt = maybeTtl
      .map { ttl =>
        val ttlInstant = currentTime.plus(ttl.toMillis, ChronoUnit.MILLIS)
        BsonDateTime(ttlInstant.toEpochMilli)
      }
      .getOrElse {
        BsonNull()
      }

    Document(
      "_id"       -> key,
      "value"     -> codec.encode(value),
      "expiresAt" -> expiresAt
    )
  }

  override protected def doPut(key: String, value: V, ttl: Option[Duration]): F[Unit] = {
    for {
      currentTime <- F.realTimeInstant

      cacheEntry = makeCacheEntry(key, value, ttl, currentTime)

      encodeAndPut = F.delay {
        collection
          .insertOne(cacheEntry)
          .head()
      }

      _ <- F.fromFuture(encodeAndPut)

    } yield ()
  }

  override protected def doRemove(key: String): F[Unit] = ???

  override protected def doRemoveAll: F[Unit] = ???

  /** You should call this when you have finished using this Cache. (e.g. when your application shuts down)
    *
    * It will take care of gracefully shutting down the underlying cache client.
    *
    * Note that you should not try to use this Cache instance after you have called this method.
    */

  override def close: F[Unit] = F.delay(client.close())
}

object MongoCache {
  def apply[F[_], V](client: MongoClient, databaseName: String, collectionName: String)(implicit
      F: Async[F],
      codec: BsonCodec[V]
  ): F[MongoCache[F, V]] = {
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

    val mongoCache = new MongoCache[F, V](client, databaseName, collectionName)

    F.fromFuture(createIndex).as(mongoCache)
  }
}
