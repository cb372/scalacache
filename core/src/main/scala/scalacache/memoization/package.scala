package scalacache

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.experimental.macros
import scala.concurrent.duration._

/**
 * Utilities for memoizing the results of method calls in a cache.
 * The cache key is generated from the method arguments using a macro,
 * so that you don't have to bother passing them manually.
 */
package object memoization {

  /**
   * Perform the given operation and memoize its result to a cache before returning it.
   * If the result is already in the cache, return it without performing the operation.
   *
   * The result is stored in the cache without a TTL, so it will remain until it is naturally evicted.
   *
   * @param f function that returns some result. This result is the valued that will be cached.
   * @param scalaCache cache configuration
   * @param flags flags to customize ScalaCache behaviour
   * @tparam A type of the value to be cached
   * @return the result, either retrieved from the cache or calculated by executing the function `f`
   */
  def memoize[A](f: => Future[A])(implicit scalaCache: ScalaCache, flags: Flags, ec: ExecutionContext): Future[A] = macro Macros.memoizeImpl[A]

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
   * @param scalaCache cache configuration
   * @param flags flags to customize ScalaCache behaviour
   * @tparam A type of the value to be cached
   * @return the result, either retrieved from the cache or calculated by executing the function `f`
   */
  def memoize[A](ttl: Duration)(f: => Future[A])(implicit scalaCache: ScalaCache, flags: Flags, ec: ExecutionContext): Future[A] = macro Macros.memoizeImplWithTTL[A]

  /**
   * Perform the given operation and memoize its result to a cache before returning it.
   * If the result is already in the cache, return it without performing the operation.
   *
   * The result is stored in the cache without a TTL, so it will remain until it is naturally evicted.
   *
   * @param f function that returns some result. This result is the valued that will be cached.
   * @param scalaCache cache configuration
   * @param flags flags to customize ScalaCache behaviour
   * @tparam A type of the value to be cached
   * @return the result, either retrieved from the cache or calculated by executing the function `f`
   */
  def memoizeSync[A](f: => A)(implicit scalaCache: ScalaCache, flags: Flags): A = macro Macros.memoizeSyncImpl[A]

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
   * @param scalaCache cache configuration
   * @param flags flags to customize ScalaCache behaviour
   * @tparam A type of the value to be cached
   * @return the result, either retrieved from the cache or calculated by executing the function `f`
   */
  def memoizeSync[A](ttl: Duration)(f: => A)(implicit scalaCache: ScalaCache, flags: Flags): A = macro Macros.memoizeSyncImplWithTTL[A]

}

