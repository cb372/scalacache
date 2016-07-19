package scalacache.aerospike

import java.util.Base64

import aerospikez.{ AerospikeClient, Hosts, Namespace }
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }
import scalacache.serialization.Codec
import scalacache.{ Cache, LoggingSupport }
import scalaz.NonEmptyList

/**
 * Created by Richard Grossman on 2016/7/18.
 */
class AerospikeCache(hostList: NonEmptyList[String],
                     namespace: Namespace = Namespace(),
                     setNameCache: String)(implicit executionContext: ExecutionContext = ExecutionContext.global)
    extends Cache[Array[Byte]] with LoggingSupport {
  override protected final val logger = LoggerFactory.getLogger(getClass.getName)

  val client = AerospikeClient(hostList)
  val set = client.setOf[String](namespace, name = setNameCache)

  logger.info(s"Connection status to Aerospike : ${client.isConnected}")
  logger.info(s"Current Nodes in cluster : ${client.getNodes}")

  /**
   * Get the value corresponding to the given key from the cache
   *
   * @param key cache key
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  override def get[V](key: String)(implicit codec: Codec[V, Array[Byte]]): Future[Option[V]] = Future {
    val bin = set.get(key).run
    bin map { b => Some(codec.deserialize(Base64.getDecoder.decode(b))) } getOrElse None
  }

  /**
   * Insert the given key-value pair into the cache, with an optional Time To Live.
   *
   * @param key   cache key
   * @param value corresponding value
   * @param ttl   Time To Live
   * @tparam V the type of the corresponding value
   */
  override def put[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V, Array[Byte]]): Future[Unit] = Future {
    val base64String = Base64.getEncoder.encodeToString(codec.serialize(value))
    set.put(key, base64String).run
  }

  /**
   * Remove the given key and its associated value from the cache, if it exists.
   * If the key is not in the cache, do nothing.
   *
   * @param key cache key
   */
  override def remove(key: String): Future[Unit] = {
    Future(if (set.exists(key).run) set.delete(key).run)
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
  def apply(hostList: Array[String], namespace: Namespace, cacheName: String) =
    new AerospikeCache(Hosts(hostList.head, hostList.tail: _*), namespace, cacheName)

  def apply(namespace: Namespace, cacheName: String): AerospikeCache =
    apply(Array("localhost:3000"), namespace, cacheName)

  def apply(cacheName: String): AerospikeCache =
    apply(Array("localhost:3000"), Namespace(), cacheName = cacheName)

}