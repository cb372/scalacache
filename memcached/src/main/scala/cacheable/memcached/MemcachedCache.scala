package cacheable.memcached

import net.spy.memcached.{AddrUtil, BinaryConnectionFactory, MemcachedClient}
import scala.concurrent.duration.Duration
import cacheable.Cache

/**
 * Author: chris
 * Created: 2/19/13
 */

class MemcachedCache(client: MemcachedClient) extends Cache with MemcachedTTLConvertor {
  val keySanitizer = new MemcachedKeySanitizer

  def get[V](key: String) =  Option(client.get(keySanitizer.toValidMemcachedKey(key)).asInstanceOf[V])

  def put[V](key: String, value: V, ttl: Option[Duration]) {
    client.set(keySanitizer.toValidMemcachedKey(key), toMemcachedExpiry(ttl), value)
  }
}

object MemcachedCache {

  /**
   * Create a Memcached client connecting to localhost:11211 and use that for caching
   */
  def apply: MemcachedCache =
    apply(new MemcachedClient(new BinaryConnectionFactory(), AddrUtil.getAddresses("localhost:11211")))

  /**
   * Create a cache that uses the given Memcached client
   * @param client Memcached client
   */
  def apply(client: MemcachedClient): MemcachedCache = new MemcachedCache(client)

}
