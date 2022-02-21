package scalacache.mongo

import cats.effect.IO
import com.mongodb.client.model.Filters
import com.mongodb.client.{MongoClients => SyncClients}
import org.bson.BsonInt32
import org.bson.BsonValue
import org.bson.Document
import org.mongodb.scala.{MongoClient => ScalaClient}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalacache.serialization.Codec
import scalacache.serialization.Codec.DecodingResult
import scalacache.serialization.bson.BsonCodec

import java.time.Instant

class MongoCacheSpec
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with IntegrationPatience {

  val collectionName = "Test-Cache"
  val databaseName   = "Test-Database"

  val mongoUri = "mongodb://localhost:27017"

  val syncClient  = SyncClients.create(mongoUri)
  val scalaClient = ScalaClient(mongoUri)

  val database   = syncClient.getDatabase(databaseName)
  val collection = database.getCollection(collectionName)

  implicit val bsonIntCodec: BsonCodec[Int] = new BsonCodec[Int] {
    override def encode(value: Int): BsonValue = new BsonInt32(value)

    override def decode(bytes: BsonValue): DecodingResult[Int] = Codec.tryDecode(bytes.asInt32().getValue)
  }

  override def beforeAll() = {
    collection.deleteMany(Filters.empty())
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

}
