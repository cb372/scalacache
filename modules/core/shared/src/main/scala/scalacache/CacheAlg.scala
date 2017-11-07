package scalacache

import scala.concurrent.duration.Duration

import scala.language.higherKinds

/**
  * Abstract algebra describing the operations a cache can perform
  *
  * @tparam V The value of types stored in the cache.
  */
trait CacheAlg[V] {

  /**
    * Get a value from the cache
    *
    * @param keyParts The cache key
    * @param mode The operation mode, which decides the type of container in which to wrap the result
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    * @return The appropriate value, if it was found in the cache
    */
  def get[F[_]](keyParts: Any*)(implicit mode: Mode[F], flags: Flags): F[Option[V]]

  /**
    * Insert a value into the cache, optionally setting a TTL (time-to-live)
    *
    * @param keyParts The cache key
    * @param value The value to insert
    * @param ttl The time-to-live. The cache entry will expire after this time has elapsed.
    * @param mode The operation mode, which decides the type of container in which to wrap the result
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    */
  def put[F[_]](keyParts: Any*)(value: V, ttl: Option[Duration] = None)(implicit mode: Mode[F], flags: Flags): F[Any]

  /**
    * Remove the given key and its associated value from the cache, if it exists.
    * If the key is not in the cache, do nothing.
    *
    * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    */
  def remove[F[_]](keyParts: Any*)(implicit mode: Mode[F]): F[Any]

  /**
    * Delete the entire contents of the cache. Use wisely!
    *
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    */
  def removeAll[F[_]]()(implicit mode: Mode[F]): F[Any]

  /**
    * Get a value from the cache if it exists. Otherwise compute it, insert it into the cache, and return it.
    *
    * @param keyParts The cache key
    * @param ttl The time-to-live to use when inserting into the cache. The cache entry will expire after this time has elapsed.
    * @param f A block that computes the value
    * @param mode The operation mode, which decides the type of container in which to wrap the result
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    * @return The value, either retrieved from the cache or computed
    */
  def caching[F[_]](keyParts: Any*)(ttl: Option[Duration])(f: => V)(implicit mode: Mode[F], flags: Flags): F[V]

  /**
    * Get a value from the cache if it exists. Otherwise compute it, insert it into the cache, and return it.
    *
    * @param keyParts The cache key
    * @param ttl The time-to-live to use when inserting into the cache. The cache entry will expire after this time has elapsed.
    * @param f A block that computes the value wrapped in a container
    * @param mode The operation mode, which decides the type of container in which to wrap the result
    * @param flags Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    * @return The value, either retrieved from the cache or computed
    */
  def cachingF[F[_]](keyParts: Any*)(ttl: Option[Duration])(f: => F[V])(implicit mode: Mode[F], flags: Flags): F[V]

  /**
    * You should call this when you have finished using this Cache.
    * (e.g. when your application shuts down)
    *
    * It will take care of gracefully shutting down the underlying cache client.
    *
    * Note that you should not try to use this Cache instance after you have called this method.
    *
    * @param mode The operation mode, which decides the type of container in which to wrap the result
    * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
    */
  def close[F[_]]()(implicit mode: Mode[F]): F[Any]

}
