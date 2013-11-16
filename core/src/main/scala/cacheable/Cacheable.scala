package cacheable

import scala.language.experimental.macros
import scala.concurrent.duration._

object Cacheable {

  /**
   * Perform the given operation and memoize its result to a cache before returning it.
   * If the result is already in the cache, return it without performing the operation.
   *
   * The result is stored in the cache without a TTL, so it will remain until it is naturally evicted.
   *
   * @param f function that returns some result. This result is the valued that will be cached.
   * @param cacheConfig cache configuration
   * @tparam A type of the value to be cached
   * @return the result, either retrieved from the cache or calculated by executing the function `f`
   */
  def cacheable[A](f: => A)(implicit cacheConfig: CacheConfig): A = macro Macros.cacheableImpl[A]

  /**
   * Perform the given operation and memoize its result to a cache before returning it.
   * If the result is already in the cache, return it without performing the operation.
   *
   * The result is stored in the cache with the given TTL. It will be evicted when the TTL is up.
   *
   * Note that if the result is currently in the cache, changing the TTL has no effect.
   * TTL is only set once, when the result is added to the cache.
   *
   * @param ttl Time To Live. How long the result should be stored in the cache.
   * @param f function that returns some result. This result is the valued that will be cached.
   * @param cacheConfig cache configuration
   * @tparam A type of the value to be cached
   * @return the result, either retrieved from the cache or calculated by executing the function `f`
   */
  def cacheable[A](ttl: Duration)(f: => A)(implicit cacheConfig: CacheConfig): A = macro Macros.cacheableImplWithTTL[A]

}

