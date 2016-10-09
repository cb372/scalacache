package scalacache.memcached

import org.slf4j.LoggerFactory
import net.spy.memcached.internal.{ OperationFuture, OperationCompletionListener }
import net.spy.memcached.{ AddrUtil, BinaryConnectionFactory, MemcachedClient }
import scalacache.serialization.Codec
import scalacache.{ LoggingSupport, Cache }

import scala.concurrent.duration.Duration

/**
 * Wrapper around spymemcached
 *
 * @param useLegacySerialization set this to true to use Spymemcached's serialization mechanism
 *                               to maintain compatibility with ScalaCache 0.7.x or earlier.
 */
class MemcachedCache(client: MemcachedClient,
                     keySanitizer: MemcachedKeySanitizer = ReplaceAndTruncateSanitizer(),
                     useLegacySerialization: Boolean = false)
    extends Cache[Array[Byte]]
    with MemcachedTTLConverter
    with LoggingSupport {

  override protected final val logger = LoggerFactory.getLogger(getClass.getName)

  override def get[V](key: String)(implicit codec: Codec[V, Array[Byte]]) = {
    val baseResult = client.get(keySanitizer.toValidMemcachedKey(key))
    val result = {
      if (baseResult != null) {
        if (useLegacySerialization)
          Some(baseResult.asInstanceOf[V])
        else
          Some(codec.deserialize(baseResult.asInstanceOf[Array[Byte]]))
      } else None
    }
    if (logger.isDebugEnabled)
      logCacheHitOrMiss(key, result)
    result
  }

  override def put[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V, Array[Byte]]) = {
    val valueToSend = if (useLegacySerialization) value else codec.serialize(value)
    val f = client.set(keySanitizer.toValidMemcachedKey(key), toMemcachedExpiry(ttl), valueToSend)
    f.addListener(new OperationCompletionListener {
      def onComplete(g: OperationFuture[_]): Unit = logCachePut(key, ttl)
    })
  }

  override def remove(key: String) = {
    client.delete(key)
  }

  override def removeAll() = {
    client.flush()
  }

  override def close(): Unit = {
    client.shutdown()
  }

}

object MemcachedCache {

  /**
   * Create a Memcached client connecting to localhost:11211 and use it for caching
   */
  def apply(): MemcachedCache = apply("localhost:11211")

  /**
   * Create a Memcached client connecting to the given host(s) and use it for caching
   *
   * @param addressString Address string, with addresses separated by spaces, e.g. "host1:11211 host2:22322"
   */
  def apply(addressString: String): MemcachedCache =
    apply(new MemcachedClient(new BinaryConnectionFactory(), AddrUtil.getAddresses(addressString)))

  /**
   * Create a cache that uses the given Memcached client
   *
   * @param client Memcached client
   */
  def apply(client: MemcachedClient): MemcachedCache = new MemcachedCache(client)

}
