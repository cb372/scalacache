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

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.mongodb.ConnectionString
import com.mongodb.MongoException
import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters
import org.bson._
import org.mongodb.scala.MongoClient
import org.mongodb.scala.MongoClientSettings
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.tagobjects.Slow
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import scalacache.serialization.Codec
import scalacache.serialization.Codec.DecodingResult
import scalacache.serialization.bson.BsonCodec
import scalacache.serialization.bson.BsonEncoder

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

class MongoCacheSpec
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with IntegrationPatience
    with Eventually {

  val collectionName = "Test-Cache"
  val databaseName   = "Test-Database"

  val mongoUri = "mongodb://localhost:27017"

  val mongoClientSettings = MongoClientSettings
    .builder()
    .applyConnectionString(new ConnectionString(mongoUri))
    .applyToSocketSettings(_.connectTimeout(5, TimeUnit.SECONDS): Unit)
    .applyToClusterSettings(_.serverSelectionTimeout(5, TimeUnit.SECONDS): Unit)
    .build()

  val mongoClient = MongoClients.create(mongoClientSettings)

  val database   = mongoClient.getDatabase(databaseName)
  val collection = database.getCollection(collectionName)

  implicit val bsonStringEncoder: BsonEncoder[String] = new BsonEncoder[String] {
    override def encode(value: String): BsonValue =
      new BsonString(value)
  }

  implicit val bsonIntCodec: BsonCodec[Int] = new BsonCodec[Int] {
    override def encode(value: Int): BsonValue =
      new BsonInt32(value)

    override def decode(bytes: BsonValue): DecodingResult[Int] =
      Codec.tryDecode(bytes.asInt32().getValue)
  }

  val mongoCache = MongoCache[IO, String, Int](mongoClientSettings, databaseName, collectionName).unsafeRunSync()

  def mongoIsRunning = {
    try {
      mongoClient
        .getDatabase("admin")
        .runCommand(new BsonDocument("ping", new BsonInt64(1)))
      true
    } catch { case _: MongoException => false }
  }

  override def afterAll() = {
    collection.drop()
    mongoClient.close()
    mongoCache.close.unsafeRunSync()
  }

  def insertCacheEntry(key: String, value: Int, expiry: Option[Instant]): Unit = {
    val document = new Document()
      .append("_id", key)
      .append("value", value)
      .append("expiresAt", expiry.orNull)

    collection.insertOne(document): Unit
  }

  if (!mongoIsRunning) {
    alert("Skipping tests because Mongodb does not appear to be running on localhost.")
  } else {

    behavior of "get"

    it should "return the value stored in Mongodb" in {
      insertCacheEntry("key1", 123, Some(Instant.now()))

      whenReady(mongoCache.get("key1").unsafeToFuture()) {
        _ should be(Some(123))
      }
    }

    it should "return None if the given key does not exist in the underlying cache" in {
      whenReady(mongoCache.get("non-existent-key").unsafeToFuture()) {
        _ should be(None)
      }
    }

    behavior of "put"

    it should "store the given key-value pair in the underlying cache" in {
      whenReady(mongoCache.put("key2")(123, None).unsafeToFuture()) { _ =>
        val document = collection.find(Filters.eq("_id", "key2")).first()

        document.getInteger("value") should be(123)
      }
    }

    behavior of "put with TTL"

    it should "store the given key-value pair in the underlying cache" taggedAs (Slow) in {
      whenReady(mongoCache.put("key3")(123, Some(3.seconds)).unsafeToFuture()) { _ =>
        val document = collection.find(Filters.eq("_id", "key3")).first()

        document.getInteger("value") should be(123)

        // MongoDB TTL expiry checks do not run extremely often
        // For more details see https://docs.mongodb.com/manual/core/index-ttl/#timing-of-the-delete-operation
        eventually(timeout(Span(65, Seconds))) {
          val document = collection.find(Filters.eq("_id", "key3")).first()

          document should be(null)
        }
      }
    }

    behavior of "remove"

    it should "delete the given key and its value from the underlying cache" in {
      insertCacheEntry("key4", 123, expiry = None)

      val document = collection.find(Filters.eq("_id", "key4")).first()
      document.getInteger("value") should be(123)

      whenReady(mongoCache.remove("key4").unsafeToFuture()) { _ =>
        val document = collection.find(Filters.eq("_id", "key4")).first()

        document should be(null)
      }
    }

    behavior of "removeAll"

    it should "delete all keys from the underlying cache" in {
      val cacheKeys      = for (idx <- 5 to 10) yield s"key$idx"
      val cacheKeyFilter = Filters.in("_id", cacheKeys: _*)

      for (key <- cacheKeys)
        insertCacheEntry(key, 123, expiry = None)

      collection.find(cacheKeyFilter).iterator().hasNext should be(true)

      whenReady(mongoCache.removeAll.unsafeToFuture()) { _ =>
        collection.find(Filters.empty()).iterator().hasNext should be(false)
      }
    }

    behavior of "resource"

    it should "ensure that the MongoClient that is created is closed" in {
      val mongoClient        = MongoClient(mongoUri)
      val mongoCacheResource = MongoCache.resource[IO, String, Int](mongoClient, databaseName, collectionName)

      whenReady(mongoCacheResource.use_.unsafeToFuture()) { _ =>
        val pingCommand = mongoClient
          .getDatabase("admin")
          .runCommand(new BsonDocument("ping", new BsonInt64(1)))
          .head()

        pingCommand.failed.futureValue shouldBe an[IllegalStateException]
      }
    }
  }

}
