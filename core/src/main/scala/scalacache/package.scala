import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }

/**
 * Author: chris
 * Created: 4/21/14
 */
package object scalacache {

  /**
   * Get the value corresponding to the given key from the cache.
   *
   * Depending on the cache implementation, this may be done synchronously or asynchronously, so it returns a Future.
   *
   * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  def get[V](keyParts: Any*)(implicit scalaCache: ScalaCache): Future[Option[V]] =
    scalaCache.cache.get(toKey(keyParts))

  /**
   * Convenience method to get a value from the cache synchronously. Warning: may block indefinitely!
   *
   * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  def getSync[V](keyParts: Any*)(implicit scalaCache: ScalaCache): Option[V] =
    Await.result(get[V](toKey(keyParts)), Duration.Inf)

  /**
   * Insert the given key-value pair into the cache, with an optional Time To Live.
   *
   * Depending on the cache implementation, this may be done synchronously or asynchronously, so it returns a Future.
   *
   * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
   * @param value the value to be cached
   * @param ttl Time To Live (optional, if not specified then the entry will be cached indefinitely)
   * @tparam V the type of the corresponding value
   */
  def put[V](keyParts: Any*)(value: V, ttl: Option[Duration] = None)(implicit scalaCache: ScalaCache): Future[Unit] =
    scalaCache.cache.put(toKey(keyParts), value, ttl)

  /**
   * Remove the given key and its associated value from the cache, if it exists.
   * If the key is not in the cache, do nothing.
   *
   * Depending on the cache implementation, this may be done synchronously or asynchronously, so it returns a Future.
   *
   * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
   */
  def remove(keyParts: Any*)(implicit scalaCache: ScalaCache): Future[Unit] =
    scalaCache.cache.remove(toKey(keyParts))

  /**
   * Wrap the given block with a caching decorator.
   * First look in the cache. If the value is found, then return it immediately.
   * Otherwise run the block and save the result in the cache before returning it.
   *
   * Note: Because no TTL is specified, the result will be stored in the cache indefinitely.
   *
   * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
   * @param f the block to run
   * @tparam V the type of the block's result
   * @return the result, either retrived from the cache or returned by the block
   */
  def caching[V](keyParts: Any*)(f: => V)(implicit scalaCache: ScalaCache): V = {
    val key = toKey(keyParts)
    getSync(key) getOrElse {
      val result = f
      scalaCache.cache.put(key, result, None)
      result
    }
  }

  /**
   * Wrap the given block with a caching decorator.
   * First look in the cache. If the value is found, then return it immediately.
   * Otherwise run the block and save the result in the cache before returning it.
   *
   * The result will be stored in the cache until the given TTL expires.
   *
   * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
   * @param ttl Time To Live
   * @param f the block to run
   * @tparam V the type of the block's result
   * @return the result, either retrived from the cache or returned by the block
   */
  def cachingWithTTL[V](keyParts: Any*)(ttl: Duration)(f: => V)(implicit scalaCache: ScalaCache): V = {
    val key = toKey(keyParts)
    getSync(key) getOrElse {
      val result = f
      scalaCache.cache.put(key, result, Some(ttl))
      result
    }
  }

  private def toKey(parts: Seq[Any])(implicit scalaCache: ScalaCache): String =
    scalaCache.keyBuilder.toCacheKey(parts)(scalaCache.cacheConfig)

}
