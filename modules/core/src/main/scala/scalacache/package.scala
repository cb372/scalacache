import scala.concurrent.duration.Duration
import scala.language.higherKinds

package object scalacache {

  /**
    * Get the value corresponding to the given key from the cache.
    *
    * @param keyParts Data to be used to generate the cache key.
    *                 This could be as simple as just a single String.
    *                 See [[CacheKeyBuilder]].
    * @param cache The cache
    * @param mode The operation mode, which decides the type of container in which to wrap the result
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    * @tparam V The type of the corresponding value
    * @return the value, if there is one
    */
  def get[F[_], V](keyParts: Any*)(implicit cache: Cache[F, V], flags: Flags): F[Option[V]] =
    cache.get(keyParts: _*)

  /**
    * Insert the given key-value pair into the cache, with an optional Time To Live.
    *
    * Depending on the cache implementation, this may be done synchronously or asynchronously, so it returns a Future.
    *
    * @param keyParts Data to be used to generate the cache key.
    *                 This could be as simple as just a single String.
    *                 See [[CacheKeyBuilder]].
    * @param value the value to be cached
    * @param ttl Time To Live (optional, if not specified then the entry will be cached indefinitely)
    * @param cache The cache
    * @param mode The operation mode, which decides the type of container in which to wrap the result
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    * @tparam V The type of the corresponding value
    */
  def put[F[_], V](
      keyParts: Any*
  )(value: V, ttl: Option[Duration] = None)(implicit cache: Cache[F, V], flags: Flags): F[Unit] =
    cache.put(keyParts: _*)(value, ttl)

  /**
    * Remove the given key and its associated value from the cache, if it exists.
    * If the key is not in the cache, do nothing.
    *
    * Depending on the cache implementation, this may be done synchronously or asynchronously, so it returns a Future.
    *
    * @param keyParts Data to be used to generate the cache key.
    *                 This could be as simple as just a single String.
    *                 See [[CacheKeyBuilder]].
    * @param cache The cache
    * @param mode The operation mode, which decides the type of container in which to wrap the result
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    * @tparam V The type of the value to be removed
    */
  def remove[F[_], V](keyParts: Any*)(implicit cache: Cache[F, V]): F[Unit] =
    cache.remove(keyParts: _*)

  final class RemoveAll[V] {
    def apply[F[_]]()(implicit cache: Cache[F, V]): F[Unit] = cache.removeAll
  }

  /**
    * Remove all values from the cache.
    *
    * @tparam V The type of values to be removed
    */
  def removeAll[V]: RemoveAll[V] = new RemoveAll[V]

  /**
    * Wrap the given block with a caching decorator.
    * First look in the cache. If the value is found, then return it immediately.
    * Otherwise run the block and save the result in the cache before returning it.
    *
    * Note: If ttl is set to None, the result will be stored in the cache indefinitely.
    *
    * @param keyParts Data to be used to generate the cache key.
    *                 This could be as simple as just a single String.
    *                 See [[CacheKeyBuilder]].
    * @param ttl The time-to-live to use when inserting into the cache.
    *            If specified, the cache entry will expire after this time has elapsed.
    * @param f The block to run
    * @param cache The cache
    * @param mode The operation mode, which decides the type of container in which to wrap the result
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    * @tparam V the type of the block's result
    * @return The result, either retrived from the cache or returned by the block
    */
  def caching[F[_], V](
      keyParts: Any*
  )(ttl: Option[Duration])(f: => V)(implicit cache: Cache[F, V], flags: Flags): F[V] =
    cache.caching(keyParts: _*)(ttl)(f)

  /**
    * Wrap the given block with a caching decorator.
    * First look in the cache. If the value is found, then return it immediately.
    * Otherwise run the block and save the result in the cache before returning it.
    *
    * Note: If ttl is set to None, the result will be stored in the cache indefinitely.
    *
    * @param keyParts Data to be used to generate the cache key.
    *                 This could be as simple as just a single String.
    *                 See [[CacheKeyBuilder]].
    * @param ttl The time-to-live to use when inserting into the cache.
    *            If specified, the cache entry will expire after this time has elapsed.
    * @param f The block to run
    * @param cache The cache
    * @param mode The operation mode, which decides the type of container in which to wrap the result
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    * @tparam V the type of the block's result
    * @return The result, either retrived from the cache or returned by the block
    */
  def cachingF[F[_], V](
      keyParts: Any*
  )(ttl: Option[Duration])(f: => F[V])(implicit cache: Cache[F, V], flags: Flags): F[V] =
    cache.cachingF(keyParts: _*)(ttl)(f)
}
