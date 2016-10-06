package scalacache

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.experimental.macros
import scala.concurrent.duration._
import scalacache.serialization.Codec

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
   * All of the above happens asynchronously, so a `Future` is returned immediately.
   * Specifically:
   * - when the cache lookup completes, if it is a miss, the function execution is started.
   * - at some point after the function completes, the result is written asynchronously to the cache.
   * - the Future returned from this method does not wait for the cache write before completing.
   *
   * The result is stored in the cache with the given TTL. It will be evicted when the TTL is up.
   *
   * Note that if the result is currently in the cache, changing the TTL has no effect.
   * TTL is only set once, when the result is added to the cache.
   *
   * @param optionalTtl Optional Time to Live. If defined, how long the result should be stored in the cache.
   * @param f function that returns some result. This result is the value that will be cached.
   * @param scalaCache cache configuration
   * @param flags flags to customize ScalaCache behaviour
   * @tparam A type of the value to be cached
   * @return the result, either retrieved from the cache or calculated by executing the function `f`
   */
  //def memoize[A, Repr](optionalTtl: Option[Duration])(f: => Future[A])(implicit scalaCache: ScalaCache[Repr], flags: Flags, ec: ExecutionContext, codec: Codec[A, Repr]): Future[A] = macro Macros.memoizeImplWithOptionalTTL[A, Repr]

}

