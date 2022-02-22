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
import com.mongodb.client.model.Filters
import com.mongodb.client.{MongoClients => SyncClients}
import org.bson.BsonInt32
import org.bson.BsonValue
import org.bson.Document
import org.mongodb.scala.{MongoClient => ScalaClient}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import scalacache.serialization.Codec
import scalacache.serialization.Codec.DecodingResult
import scalacache.serialization.bson.BsonCodec

import scala.concurrent.duration._
import java.time.Instant

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

  val syncClient  = SyncClients.create(mongoUri)
  val scalaClient = ScalaClient(mongoUri)

  val database   = syncClient.getDatabase(databaseName)
  val collection = database.getCollection(collectionName)

  implicit val bsonIntCodec: BsonCodec[Int] = new BsonCodec[Int] {
    override def encode(value: Int): BsonValue =
      new BsonInt32(value)

    override def decode(bytes: BsonValue): DecodingResult[Int] =
      Codec.tryDecode(bytes.asInt32().getValue)
  }

  override def beforeAll() = {
    collection.drop()
  }

  override def afterAll() = {
    syncClient.close()
  }

  import cats.effect.unsafe.implicits.global

  val mongoCache = new MongoCache[IO, Int](scalaClient, databaseName, collectionName)

  behavior of "get"

  it should "return the value stored in Mongodb" in {
    val document = new Document()
      .append("_id", "key1")
      .append("value", 123)
      .append("expiresAt", Instant.now)

    collection.insertOne(document)

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

  it should "store the given key-value pair in the underlying cache" in {
    whenReady(MongoCache[IO, Int](scalaClient, databaseName, collectionName).unsafeToFuture()) { mongoCache =>
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
  }

}
