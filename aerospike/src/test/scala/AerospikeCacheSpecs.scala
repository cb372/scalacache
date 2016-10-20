import java.util.concurrent.TimeUnit

import com.aerospike.client.policy.{BatchPolicy, WritePolicy}
import com.aerospike.client.{AerospikeClient, Bin, Key}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.duration.Duration
import scalacache.aerospike.AerospikeCache

class AerospikeCacheSpecs extends FlatSpec with Matchers with BeforeAndAfter with ScalaFutures {
  val client = new AerospikeClient("127.0.0.1", 3000)
  val cacheName = "unittest"
  val namespace = "test"
  val writePolicy = new WritePolicy()
  val readPolicy = new BatchPolicy()
  val cache = AerospikeCache(Array("127.0.0.1:3000"), namespace, cacheName)

  def aerospikeIsRunning = client.isConnected

  if (!aerospikeIsRunning) {
    alert("Skipping tests because Aerospike does not appear to be running on localhost.")
  } else {
    behavior of "get"
    it should "retrieve a value stored in Aerospike" in {
      val key = new Key(namespace, cacheName, "key1")
      val bin = new Bin("cache", "value for key 1".getBytes)
      client.put(writePolicy, key, bin)

      whenReady(cache.get[String]("key1")) {_ should be(Some("value for key 1"))}
    }

    it should "return None if the key doesn't exists" in {
      whenReady(cache.get[String]("NotExists")) {_ should be(None)}
    }

    behavior of "put"
    it should "Add a new key,value to aerospike as string" in {
      whenReady(cache.put[String]("key2", "Value to cache", None)) { _ =>
        val key = new Key(namespace, cacheName, "key2")
        val record = client.get(readPolicy, key)
        new String(record.bins.get("cache").asInstanceOf[Array[Byte]]) should be("Value to cache")
      }
    }

    it should "add a value and check is TTL is set" in {
      whenReady(cache.put("key3", "Value to cache", Some(Duration(2, TimeUnit.SECONDS)))) { _ =>
        Thread.sleep(5000) // sleep 5 second ttl have occured
        val key = new Key(namespace, cacheName, "key3")

        val record = client.get(readPolicy, key)
        Option(record) should be(None)
      }
    }

    behavior of "remove"
    it should "remove a value from the cache given a key" in {
      val key = new Key(namespace, cacheName, "key4")
      val bin = new Bin("cache", "value to be removed")
      client.put(writePolicy, key, bin)

      whenReady(cache.remove("key4")) { _ =>
        val record = client.get(readPolicy, key)
        Option(record) should be(None)
      }
    }
  }

}
