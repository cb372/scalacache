package scalacache.aerospike

import com.aerospike.client.async.{AsyncClient, AsyncClientPolicy}
import com.aerospike.client.policy.{BatchPolicy, WritePolicy}
import com.aerospike.client.{Bin, Host, Key}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scalacache.serialization.Codec
import scalacache.{Cache, LoggingSupport}

class AerospikeCache(hostList: Array[Host],
                     namespace: String = "",
                     cacheName: String,
                     writePolicy: WritePolicy,
                     readPolicy: BatchPolicy)(implicit executionContext: ExecutionContext = ExecutionContext.global)
    extends Cache[Array[Byte]] with LoggingSupport {
  override protected implicit val logger = LoggerFactory.getLogger(getClass.getName)

  private val policy = new AsyncClientPolicy()
  policy.threadPool = ExecutionContextExecutorServiceBridge(executionContext)
  implicit val client = new AsyncClient(policy, hostList :_*)

  logger.info(s"Connection status to Aerospike : ${client.isConnected}")
  logger.info(s"Current Nodes in cluster : ${client.getNodes}")

  /**
   * Get the value corresponding to the given key from the cache
   *
   * @param key cache key
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  override def get[V](key: String)(implicit codec: Codec[V, Array[Byte]]): Future[Option[V]] = {
    val p = Promise[Option[Array[Byte]]]
    val aerospikeKey = new Key(namespace, cacheName, key)
    client.get(readPolicy, new ReadHandler(p), aerospikeKey)
    p.future map (f => f map codec.deserialize)
  }

  /**
   * Insert the given key-value pair into the cache, with an optional Time To Live.
   *
   * @param key   cache key
   * @param value corresponding value
   * @param ttl   Time To Live
   * @tparam V the type of the corresponding value
   */
  override def put[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V, Array[Byte]]): Future[Unit] = {
    val p = Promise[Unit]()
    val aerospikeKey = new Key(namespace, cacheName, key)
    val bin = new Bin("cache", codec.serialize(value))
    ttl foreach {d => writePolicy.expiration = d.toSeconds.toInt}
    client.put(writePolicy, new WriteHandler(p), aerospikeKey, bin)
    p.future
  }

  /**
   * Remove the given key and its associated value from the cache, if it exists.
   * If the key is not in the cache, do nothing.
   *
   * @param key cache key
   */
  override def remove(key: String): Future[Unit] = {
    val p = Promise[Unit]()
    val aerospikeKey = new Key(namespace, cacheName, key)
    client.delete(writePolicy, new DeleteHandler(p), aerospikeKey)
    p.future
  }

  /**
   * Delete the entire contents of the cache. Use wisely!
   */
  override def removeAll(): Future[Unit] = throw new Exception("Not Supported")

  /**
   * You should call this when you have finished using this Cache.
   * (e.g. when your application shuts down)
   *
   * It will take care of gracefully shutting down the underlying cache client.
   *
   * Note that you should not try to use this Cache instance after you have called this method.
   */
  override def close(): Unit = {
    client.close
    logger.info(s"Connection to aerospike is ${client.isConnected}")
  }

}

object AerospikeCache {
  def apply(hostList: Array[String], namespace: String, cacheName: String) : AerospikeCache =
    apply(hostList, namespace, cacheName, new WritePolicy(), new BatchPolicy())

  def apply(hostList: Array[String], namespace: String, cacheName: String,
            writePolicy: WritePolicy, batchPolicy: BatchPolicy): AerospikeCache = {
    val hosts = hostList map { h =>
      val t = h.split(":")
      new Host(t(0), t(1).toInt)
    }
    new AerospikeCache(hosts, namespace, cacheName, writePolicy, batchPolicy)
  }


}

