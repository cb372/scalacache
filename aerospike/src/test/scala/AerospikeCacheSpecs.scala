import java.util.Base64

import aerospikez.{AerospikeClient, Namespace, WriteConfig}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scalacache.aerospike.AerospikeCache

/**
  * Created by Richard Grossman on 2016/7/18.
  */
class AerospikeCacheSpecs extends FlatSpec with Matchers with BeforeAndAfter with ScalaFutures {
  val client = AerospikeClient()
  val cacheName = "unittest"
  val set = client.setOf[String](Namespace(), name = cacheName)

  def aerospikeIsRunning = client.isConnected

  if (!aerospikeIsRunning) {
    alert("Skipping tests because Aerospike does not appear to be running on localhost.")
  } else {
    behavior of "get"
    it should "retrieve a value stored in Aerospike" in {
      val base64String = Base64.getEncoder.encodeToString("value for key 1".getBytes)
      set.put("key1", base64String).run
      val cache = AerospikeCache(cacheName)
      whenReady(cache.get[String]("key1")) {_ should be(Some("value for key 1"))}
    }

    it should "return None if the key doesn't exists" in {
      val cache = AerospikeCache(cacheName)
      whenReady(cache.get[String]("NotExists")) {_ should be(None)}
    }

    behavior of "put"
    it should "Add a new key,value to aerospike the string is base64 encoded" in {
      val cache = AerospikeCache(cacheName)
      whenReady(cache.put("key1", "Value to cache", None)) { _ =>
        set.get("key1").run should be(Some("VmFsdWUgdG8gY2FjaGU="))
      }
    }

    it should "add a value and check is TTL is set" in {
      val namespace = Namespace("test", writeConfig = WriteConfig(expiration = 10))
      val setWithTTL = client.setOf[String](namespace, cacheName)
      val cache = AerospikeCache(namespace, cacheName)

      whenReady(cache.put("key1", "Value to cache", None)) { _ =>
        Thread.sleep(20000) // sleep 40 second ttl have occured
        setWithTTL.get("key1").run should be(None)
      }
    }

    behavior of "remove"
    it should "remove a value from the cache given a key" in {
      set.put("key1", "this value must be removed").run
      val cache = AerospikeCache(cacheName)
      whenReady(cache.remove("key1")) { _ =>
        set.get("key1").run should be(None)
      }
    }
  }

}
