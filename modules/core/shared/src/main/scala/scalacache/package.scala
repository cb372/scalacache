import scalacache.serialization.Codec

import scala.concurrent.duration.Duration
import scala.language.higherKinds

package object scalacache {

  type Id[X] = X

  /**
    * Get the value corresponding to the given key from the cache.
    *
    * @param keyParts Data to be used to generate the cache key.
    *                 This could be as simple as just a single String.
    *                 See [[CacheKeyBuilder]].
    * @param cache The cache
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    * @tparam V The type of the corresponding value
    * @return the value, if there is one
    */
  def get[F[_], V: Codec](keyParts: Any*)(implicit cache: Cache[F], flags: Flags): F[Option[V]] =
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
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    * @tparam V The type of the corresponding value
    */
  def put[F[_], V: Codec](keyParts: Any*)(value: V, ttl: Option[Duration] = None)(implicit cache: Cache[F],
                                                                                  flags: Flags): F[Any] =
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
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    */
  def remove[F[_]](keyParts: Any*)(implicit cache: Cache[F]): F[Any] =
    cache.remove(keyParts: _*)

  // TODO Jules: ???
  final class RemoveAll {
    def apply[F[_]]()(implicit cache: Cache[F]): F[Any] = cache.removeAll()
  }

  /**
    * Remove all values from the cache.
    */
  def removeAll(): RemoveAll = new RemoveAll

  /**
    * Wrap the given block with a caching decorator.
    * First look in the cache. If the value is found, then return it immediately.
    * Otherwise run the block and save the result in the cache before returning it.
    *
    * Note: Because no TTL is specified, the result will be stored in the cache indefinitely.
    *
    * @param keyParts Data to be used to generate the cache key.
    *                 This could be as simple as just a single String.
    *                 See [[CacheKeyBuilder]].
    * @param ttl The time-to-live to use when inserting into the cache.
    *            If specified, the cache entry will expire after this time has elapsed.
    * @param f The block to run
    * @param cache The cache
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    * @tparam V the type of the block's result
    * @return The result, either retrived from the cache or returned by the block
    */
  def caching[F[_], V: Codec](keyParts: Any*)(ttl: Option[Duration])(f: => V)(implicit cache: Cache[F],
                                                                              flags: Flags): F[V] =
    cache.caching(keyParts: _*)(ttl)(f)

  /**
    * Wrap the given block with a caching decorator.
    * First look in the cache. If the value is found, then return it immediately.
    * Otherwise run the block and save the result in the cache before returning it.
    *
    * Note: Because no TTL is specified, the result will be stored in the cache indefinitely.
    *
    * @param keyParts Data to be used to generate the cache key.
    *                 This could be as simple as just a single String.
    *                 See [[CacheKeyBuilder]].
    * @param ttl The time-to-live to use when inserting into the cache.
    *            If specified, the cache entry will expire after this time has elapsed.
    * @param f The block to run
    * @param cache The cache
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    * @tparam V the type of the block's result
    * @return The result, either retrived from the cache or returned by the block
    */
  def cachingF[F[_], V: Codec](keyParts: Any*)(ttl: Option[Duration])(f: => F[V])(implicit cache: Cache[F],
                                                                                  flags: Flags): F[V] =
    cache.cachingF(keyParts: _*)(ttl)(f)

  /**
    * A version of the API that is specialised to [[Id]].
    * The functions in this API perform their operations immediately
    * on the current thread and thus do not wrap their results in any effect monad.
    *
    * ===
    *
    * Implementation note: I really didn't want to have this separate copy of the API,
    * but I couldn't get type inference to understand that Id[A] == A.
    * e.g. the following doesn't compile:
    *
    * implicit val cache: LovelyCache[String] = ???
    * import scalacache.modes.sync._
    * val x: Option[String] = scalacache.get("hello")
    *
    * [error] ... polymorphic expression cannot be instantiated to expected type;
    * [error]  found   : [F[_], V]F[Option[V]]
    * [error]  required: Option[String]
    *
    * If anyone can find a workaround to make this compile, I will be much obliged.
    */
  object sync {

    def get[V: Codec](keyParts: Any*)(implicit cache: Cache[Id], flags: Flags): Option[V] =
      cache.get[V](keyParts: _*)

    def put[V: Codec](keyParts: Any*)(value: V, ttl: Option[Duration] = None)(implicit cache: Cache[Id],
                                                                              flags: Flags): Any =
      cache.put[V](keyParts: _*)(value, ttl)

    def remove(keyParts: Any*)(implicit cache: Cache[Id]): Any =
      cache.remove(keyParts: _*)

    def caching[V: Codec](keyParts: Any*)(ttl: Option[Duration])(f: => V)(implicit cache: Cache[Id], flags: Flags): V =
      cache.caching[V](keyParts: _*)(ttl)(f)
  }

}
