import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

package object scalacache extends StrictLogging {

  /**
   * Get the value corresponding to the given key from the cache.
   *
   * Depending on the cache implementation, this may be done synchronously or asynchronously, so it returns a Future.
   *
   * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  def get[V](keyParts: Any*)(implicit scalaCache: ScalaCache, flags: Flags): Future[Option[V]] = getWithKey(toKey(keyParts))

  /**
   * Convenience method to get a value from the cache synchronously. Warning: may block indefinitely!
   *
   * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  def getSync[V](keyParts: Any*)(implicit scalaCache: ScalaCache, flags: Flags): Option[V] = getSyncWithKey(toKey(keyParts))

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
  def put[V](keyParts: Any*)(value: V, ttl: Option[Duration] = None)(implicit scalaCache: ScalaCache, flags: Flags): Future[Unit] =
    putWithKey(toKey(keyParts), value, ttl)

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
  def caching[V](keyParts: Any*)(f: => V)(implicit scalaCache: ScalaCache, flags: Flags): V = {
    val key = toKey(keyParts)
    getSyncWithKey(key) getOrElse {
      val result = f
      putWithKey(key, result, None)
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
  def cachingWithTTL[V](keyParts: Any*)(ttl: Duration)(f: => V)(implicit scalaCache: ScalaCache, flags: Flags): V = {
    val key = toKey(keyParts)
    getSyncWithKey(key) getOrElse {
      val result = f
      putWithKey(key, result, Some(ttl))
      result
    }
  }

  private def toKey(parts: Seq[Any])(implicit scalaCache: ScalaCache): String =
    scalaCache.keyBuilder.toCacheKey(parts)(scalaCache.cacheConfig)

  private def getWithKey[V](key: String)(implicit scalaCache: ScalaCache, flags: Flags): Future[Option[V]] = {
    if (flags.readsEnabled) {
      scalaCache.cache.get(key)
    } else {
      logger.debug(s"Skipping cache GET because cache reads are disabled. Key: $key")
      Future.successful(None)
    }
  }

  private def getSyncWithKey[V](key: String)(implicit scalaCache: ScalaCache, flags: Flags): Option[V] =
    Await.result(getWithKey(key), Duration.Inf)

  def putWithKey[V](key: String, value: V, ttl: Option[Duration] = None)(implicit scalaCache: ScalaCache, flags: Flags): Future[Unit] = {
    if (flags.writesEnabled) {
      scalaCache.cache.put(key, value, ttl)
    } else {
      logger.debug(s"Skipping cache PUT because cache writes are disabled. Key: $key")
      Future.successful(())
    }
  }

}
