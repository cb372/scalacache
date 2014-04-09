package cacheable.memcached

import net.spy.memcached.{AddrUtil, BinaryConnectionFactory, MemcachedClient}
import scala.concurrent.duration.Duration
import cacheable.{LoggingSupport, Cache}
import com.typesafe.scalalogging.slf4j.{LazyLogging, StrictLogging}

/**
 * Author: chris
 * Created: 2/19/13
 */

class MemcachedCache(client: MemcachedClient)
    extends Cache
    with MemcachedTTLConvertor
    with StrictLogging
    with LoggingSupport {

  val keySanitizer = new MemcachedKeySanitizer

  def get[V](key: String) = {
    val result = Option(client.get(keySanitizer.toValidMemcachedKey(key)).asInstanceOf[V])
    logCacheHitOrMiss(key, result)
    result
  }

  def put[V](key: String, value: V, ttl: Option[Duration]) {
    client.set(keySanitizer.toValidMemcachedKey(key), toMemcachedExpiry(ttl), value)
    logCachePut(key, ttl)
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
