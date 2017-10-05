package scalacache

import scala.concurrent.duration.Duration

import scala.language.higherKinds

/**
  * Abstract algebra describing the operations a cache can perform
  *
  * The type parameters may look a little intimidating, so let's explain them:
  *
  * [[V]] is the type of all values stored in the cache.
  *
  * [[S]] is a type class describing what operations `F` must support,
  * where `F` is the container decided by the mode.
  * So [[S]] restricts what modes are compatible with this cache.
  *
  * Basically [[S]] describes whether the cache is implemented in a synchronous (i.e. blocking) way,
  * or asynchronously using a callback mechanism.
  *
  * For synchronous caches, e.g. Caffeine, [[S]] will be [[Sync]].
  * For asynchronous caches implemented using callbacks, e.g. Memcached, [[S]] will be [[Async]].
  *
  * @tparam V The value of types stored in the cache.
  * @tparam S A type class describing what operations a container `F` must support in order to be used with this cache.
  */
trait CacheAlg[V, S[F[_]] <: Sync[F]] {

  /**
    * Get a value from the cache
    *
    * @param keyParts The cache key
    * @param mode The operation mode, which decides the type of container in which to wrap the result
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam G The type of container in which the result will be wrapped. This is decided by the mode.
    * @return The appropriate value, if it was found in the cache
    */
  def get[F[_], G[_]](keyParts: Any*)(implicit mode: Mode[F, G, S], flags: Flags): G[Option[V]]

  /**
    * Insert a value into the cache, optionally setting a TTL (time-to-live)
    *
    * @param keyParts The cache key
    * @param value The value to insert
    * @param ttl The time-to-live. The cache entry will expire after this time has elapsed.
    * @param mode The operation mode, which decides the type of container in which to wrap the result
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam G The type of container in which the result will be wrapped. This is decided by the mode.
    */
  def put[F[_], G[_]](keyParts: Any*)(value: V, ttl: Option[Duration] = None)(implicit mode: Mode[F, G, S],
                                                                              flags: Flags): G[_]

  /**
    * Remove the given key and its associated value from the cache, if it exists.
    * If the key is not in the cache, do nothing.
    *
    * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
    */
  def remove[F[_], G[_]](keyParts: Any*)(implicit mode: Mode[F, G, S]): G[_]

  /**
    * Delete the entire contents of the cache. Use wisely!
    */
  def removeAll[F[_], G[_]]()(implicit mode: Mode[F, G, S]): G[_]

  /**
    * Get a value from the cache if it exists. Otherwise compute it, insert it into the cache, and return it.
    *
    * @param keyParts The cache key
    * @param ttl The time-to-live to use when inserting into the cache. The cache entry will expire after this time has elapsed.
    * @param f A block that computes the value
    * @param mode The operation mode, which decides the type of container in which to wrap the result
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam G The type of container in which the result will be wrapped. This is decided by the mode.
    * @return The value, either retrieved from the cache or computed
    */
  def caching[F[_], G[_]](keyParts: Any*)(ttl: Option[Duration] = None)(f: => V)(implicit mode: Mode[F, G, S],
                                                                                 flags: Flags): G[V]

  /**
    * Get a value from the cache if it exists. Otherwise compute it, insert it into the cache, and return it.
    *
    * @param keyParts The cache key
    * @param ttl The time-to-live to use when inserting into the cache. The cache entry will expire after this time has elapsed.
    * @param f A block that computes the value wrapped in a container
    * @param mode The operation mode, which decides the type of container in which to wrap the result
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam G The type of container in which the result will be wrapped. This is decided by the mode.
    * @return
    */
  def cachingF[F[_], G[_]](keyParts: Any*)(ttl: Option[Duration] = None)(f: => F[V])(implicit mode: Mode[F, G, S],
                                                                                     flags: Flags): G[V]

  // optimised methods for use by memoize: we know the key will be a single string so we can avoid some work

  private[scalacache] def cachingForMemoize[F[_], G[_]](baseKey: String)(ttl: Option[Duration])(
      f: => V)(implicit mode: Mode[F, G, S], flags: Flags): G[V]

  private[scalacache] def cachingForMemoizeF[F[_], G[_]](baseKey: String)(ttl: Option[Duration])(
      f: => F[V])(implicit mode: Mode[F, G, S], flags: Flags): G[V]
}
