package scalacache

import scala.language.experimental.macros
import scala.concurrent.duration._
import scala.language.higherKinds

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
    * If a TTL is given, the result is stored in the cache with that TTL.
    * It will be evicted when the TTL is up.
    *
    * Note that if the result is currently in the cache, changing the TTL has no effect.
    * TTL is only set once, when the result is added to the cache.
    *
    * @param ttl Time-To-Live
    * @param f A function that computes some result. This result is the value that will be cached.
    * @param mode The operation mode, which decides the type of container in which to wrap the result
    * @param cache The cache
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    * @tparam V The type of the value to be cached
    * @return A result, either retrieved from the cache or calculated by executing the function `f`
    */
  def memoize[F[_], V](ttl: Option[Duration])(f: => V)(implicit cache: Cache[V], mode: Mode[F], flags: Flags): F[V] =
    macro Macros.memoizeImpl[F, V]

  /**
    * Perform the given operation and memoize its result to a cache before returning it.
    * If the result is already in the cache, return it without performing the operation.
    *
    * If a TTL is given, the result is stored in the cache with that TTL.
    * It will be evicted when the TTL is up.
    *
    * Note that if the result is currently in the cache, changing the TTL has no effect.
    * TTL is only set once, when the result is added to the cache.
    *
    * @param ttl Time-To-Live
    * @param f A function that computes some result wrapped in an [[F]]. This result is the value that will be cached.
    * @param mode The operation mode, which decides the type of container in which to wrap the result
    * @param cache The cache
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    * @tparam V The type of the value to be cached
    * @return A result, either retrieved from the cache or calculated by executing the function `f`
    */
  def memoizeF[F[_], V](ttl: Option[Duration])(
      f: => F[V])(implicit cache: Cache[V], mode: Mode[F], flags: Flags): F[V] =
    macro Macros.memoizeFImpl[F, V]

  /**
    * A version of [[memoize]] that is specialised to [[Id]].
    * This is provided for convenience because type inference doesn't work properly for [[Id]],
    * and writing `memoize[Id, Foo]` is a bit rubbish.
    *
    * Perform the given operation and memoize its result to a cache before returning it.
    * If the result is already in the cache, return it without performing the operation.
    *
    * If a TTL is given, the result is stored in the cache with that TTL.
    * It will be evicted when the TTL is up.
    *
    * Note that if the result is currently in the cache, changing the TTL has no effect.
    * TTL is only set once, when the result is added to the cache.
    *
    * @param ttl Time-To-Live
    * @param f A function that computes some result. This result is the value that will be cached.
    * @param cache The cache
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam V The type of the value to be cached
    * @return A result, either retrieved from the cache or calculated by executing the function `f`
    */
  def memoizeSync[V](ttl: Option[Duration])(f: => V)(implicit cache: Cache[V], mode: Mode[Id], flags: Flags): V =
    macro Macros.memoizeSyncImpl[V]

}
