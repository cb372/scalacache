package scalacache.memcached

import net.spy.memcached.{ AddrUtil, BinaryConnectionFactory, MemcachedClient }
import scala.concurrent.duration.Duration
import scalacache.{ LoggingSupport, Cache }
import com.typesafe.scalalogging.slf4j.{ LazyLogging, StrictLogging }
import scala.concurrent.{ Future, ExecutionContext }

/**
 * Wrapper around spymemcached
 *
 * Author: chris
 * Created: 2/19/13
 */
class MemcachedCache(client: MemcachedClient)
    extends Cache
    with MemcachedTTLConvertor
    with StrictLogging
    with LoggingSupport {

  val keySanitizer = new MemcachedKeySanitizer

  /**
   * Get the value corresponding to the given key from the cache
   * @param key cache key
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  override def get[V](key: String)(implicit execContext: ExecutionContext) = Future {
    val result = Option(client.get(keySanitizer.toValidMemcachedKey(key)).asInstanceOf[V])
    logCacheHitOrMiss(key, result)
    result
  }

  /**
   * Insert the given key-value pair into the cache, with an optional Time To Live.
   * @param key cache key
   * @param value corresponding value
   * @param ttl Time To Live
   * @tparam V the type of the corresponding value
   */
  override def put[V](key: String, value: V, ttl: Option[Duration])(implicit execContext: ExecutionContext) = Future {
    client.set(keySanitizer.toValidMemcachedKey(key), toMemcachedExpiry(ttl), value)
    logCachePut(key, ttl)
  }

  /**
   * Remove the given key and its associated value from the cache, if it exists.
   * If the key is not in the cache, do nothing.
   * @param key cache key
   */
  override def remove(key: String)(implicit execContext: ExecutionContext) = Future {
    client.delete(key)
  }

}

object MemcachedCache {

  /**
   * Create a Memcached client connecting to localhost:11211 and use it for caching
   */
  def apply(): MemcachedCache = apply("localhost:11211")

  /**
   * Create a Memcached client connecting to the given host(s) and use it for caching
   * @param addressString Address string, with addresses separated by spaces, e.g. "host1:11211 host2:22322"
   */
  def apply(addressString: String): MemcachedCache =
    apply(new MemcachedClient(new BinaryConnectionFactory(), AddrUtil.getAddresses(addressString)))

  /**
   * Create a cache that uses the given Memcached client
   * @param client Memcached client
   */
  def apply(client: MemcachedClient): MemcachedCache = new MemcachedCache(client)

}
