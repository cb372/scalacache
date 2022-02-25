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
import com.mongodb.MongoClientSettings
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import org.bson.BsonDateTime
import org.bson.BsonNull
import org.bson.Document
import org.bson.conversions.Bson
import org.reactivestreams.Publisher
import org.reactivestreams.Subscription
import reactor.core.publisher.Mono
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

  private def keyFilter(key: K): Bson =
    Filters.eq("_id", keyEncoder.encode(key))

  override protected def doGet(key: K): F[Option[V]] = {
    val findCommand = F.delay(collection.find(keyFilter(key))).widen[Publisher[Document]]

    MongoCache.monoSubscriber[F, Document, Option[V]](findCommand)(
      onComplete = cb => cb(Right(None)),
      onValue = cb => { doc =>
        val bsonDocument = doc.toBsonDocument()
        val valueBson    = bsonDocument.get("value")
        val decodeResult = valueCodec.decode(valueBson).map(Some(_))
        cb(decodeResult)
      }
    )
  }

  private def makeCacheEntry(key: K, value: V, maybeTtl: Option[Duration], currentTime: Instant): Document = {
    val expiresAt = maybeTtl
      .map { ttl =>
        val ttlInstant = currentTime.plus(ttl.toMillis, ChronoUnit.MILLIS)
        new BsonDateTime(ttlInstant.toEpochMilli)
      }
      .getOrElse {
        BsonNull.VALUE
      }

    new Document()
      .append("_id", keyEncoder.encode(key))
      .append("value", valueCodec.encode(value))
      .append("expiresAt", expiresAt)
  }

  override protected def doPut(key: K, value: V, ttl: Option[Duration]): F[Unit] = {
    for {
      currentTime <- F.realTimeInstant

      cacheEntry = makeCacheEntry(key, value, ttl, currentTime)

      upsert = new ReplaceOptions().upsert(true)

      putCommand = F.delay(collection.replaceOne(keyFilter(key), cacheEntry, upsert))

      _ <- MongoCache.voidSubscriber(putCommand)

    } yield ()
  }

  override protected def doRemove(key: K): F[Unit] = {
    val removeCommand = F.delay(collection.deleteOne(keyFilter(key)))
    MongoCache.voidSubscriber(removeCommand)
  }

  override protected def doRemoveAll: F[Unit] = {
    val removeAllCommand = F.delay(collection.deleteMany(Filters.empty()))
    MongoCache.voidSubscriber(removeAllCommand)
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
    val mongoClient = MongoClients.create(connectionString)
    apply(mongoClient, databaseName, collectionName)
  }

  def apply[F[_], K, V](mongoClientSettings: MongoClientSettings, databaseName: String, collectionName: String)(implicit
      F: Async[F],
      keyEncoder: BsonEncoder[K],
      valueCodec: BsonCodec[V]
  ): F[MongoCache[F, K, V]] = {
    val mongoClient = MongoClients.create(mongoClientSettings)
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
    val indexOptions = new IndexOptions().expireAfter(0, TimeUnit.MILLISECONDS)

    val createIndexCommand = F.delay(collection.createIndex(indexName, indexOptions))
    val createIndex        = MongoCache.voidSubscriber(createIndexCommand)

    createIndex.as {
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
    val mongoClient = MongoClients.create(connectionString)
    resource(mongoClient, databaseName, collectionName)
  }

  def resource[F[_], K, V](mongoClientSettings: MongoClientSettings, databaseName: String, collectionName: String)(
      implicit
      F: Async[F],
      keyEncoder: BsonEncoder[K],
      valueCodec: BsonCodec[V]
  ): Resource[F, MongoCache[F, K, V]] = {
    val mongoClient = MongoClients.create(mongoClientSettings)
    resource(mongoClient, databaseName, collectionName)
  }

  private[mongo] def resource[F[_], K, V](client: MongoClient, databaseName: String, collectionName: String)(implicit
      F: Async[F],
      keyEncoder: BsonEncoder[K],
      valueCodec: BsonCodec[V]
  ): Resource[F, MongoCache[F, K, V]] =
    Resource.make(apply[F, K, V](client, databaseName, collectionName))(_.close)

  private[mongo] def voidSubscriber[F[_], A](publisher: F[Publisher[A]])(implicit F: Async[F]): F[Unit] =
    monoSubscriber(publisher)(
      onValue = cb => _ => cb(Right(())),
      onComplete = _ => {}
    )

  private[mongo] def monoSubscriber[F[_], A, B](publisher: F[Publisher[A]])(
      onValue: (Either[Throwable, B] => Unit) => A => Unit,
      onComplete: (Either[Throwable, B] => Unit) => Unit
  )(implicit F: Async[F]): F[B] = {
    F.async[B] { cb =>
      publisher.map { pub =>
        val subscription = Mono
          .from(pub)
          .subscribe(
            (value: A) => onValue(cb)(value),
            (error: Throwable) => cb(Left(error)),
            () => onComplete(cb),
            (sub: Subscription) => sub.request(1)
          )

        // Cancel the subscription if the fiber is cancelled
        Some(F.delay(subscription.dispose()))
      }
    }
  }

}
