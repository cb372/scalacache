
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.Try
import scalacache.serialization.{DummyCodec, Codec, JavaSerializationCodec}
import scalacache.util.SingletonOf

package object scalacache extends StrictLogging with JavaSerializationCodec with DummyCodec {


  class TypedApi[V, foo <: { type CodecTarget }](implicit val sc: SingletonOf[ScalaCache, foo], scalaCache: ScalaCache, codec: Codec[V, foo#CodecTarget]) {

    def get(keyParts: Any*)(implicit flags: Flags): Future[Option[V]] = getWithKey(toKey(keyParts))

    def put(keyParts: Any*)(value: V, ttl: Option[Duration] = None)(implicit flags: Flags): Future[Unit] =
      putWithKey(toKey(keyParts), value, ttl)

    def remove(keyParts: Any*): Future[Unit] =
      scalacache.remove(keyParts: _*)

    def removeAll(): Future[Unit] =
      scalacache.removeAll()

    def caching(keyParts: Any*)(f: => Future[V])(implicit flags: Flags, execContext: ExecutionContext = ExecutionContext.global): Future[V] = {
      _caching(keyParts: _*)(None)(f)
    }

    def cachingWithTTL(keyParts: Any*)(ttl: Duration)(f: => Future[V])(implicit flags: Flags, execContext: ExecutionContext = ExecutionContext.global): Future[V] = {
      _caching(keyParts: _*)(Some(ttl))(f)
    }

    def cachingWithOptionalTTL(keyParts: Any*)(optionalTtl: Option[Duration])(f: => Future[V])(implicit flags: Flags, execContext: ExecutionContext = ExecutionContext.global): Future[V] = {
      _caching(keyParts: _*)(optionalTtl)(f)
    }

    private def _caching(keyParts: Any*)(ttl: Option[Duration])(f: => Future[V])(implicit flags: Flags, execContext: ExecutionContext): Future[V] = {
      val key = toKey(keyParts)

      def asynchronouslyCacheResult(result: Future[V]): Unit = result onSuccess {
        case computedValue =>
          Try(putWithKey(key, computedValue, ttl)) recover {
            case e => logger.warn(s"Failed to write to cache. Key = $key", e)
          }
      }

      val fromCache: Future[Option[V]] = getWithKey(key).recover[Option[V]] {
        case e =>
          logger.warn(s"Failed to read from cache. Key = $key", e)
          None
      }

      fromCache flatMap {
        case Some(value) => Future.successful(value)
        case None =>
          val result: Future[V] = f
          asynchronouslyCacheResult(result)
          result
      }
    }

    private def getWithKey(key: String)(implicit flags: Flags): Future[Option[V]] = {
      if (flags.readsEnabled) {
        scalaCache.cache.get[V](key)
      } else {
        logger.debug(s"Skipping cache GET because cache reads are disabled. Key: $key")
        Future.successful(None)
      }
    }

    private def putWithKey(key: String, value: V, ttl: Option[Duration] = None)(implicit flags: Flags): Future[Unit] = {
      if (flags.writesEnabled) {
        val finiteTtl = ttl.filter(_.isFinite()) // discard Duration.Inf, Duration.Undefined
        scalaCache.cache.put(key, value, finiteTtl)
      } else {
        logger.debug(s"Skipping cache PUT because cache writes are disabled. Key: $key")
        Future.successful(())
      }
    }

    /**
     * Synchronous API, for the case when you don't want to deal with Futures.
     */
    object sync {

      def get(keyParts: Any*)(implicit flags: Flags): Option[V] = getSyncWithKey(toKey(keyParts))

      def caching(keyParts: Any*)(f: => V)(implicit flags: Flags): V = {
        _cachingSync(keyParts: _*)(None)(f)
      }

      def cachingWithTTL(keyParts: Any*)(ttl: Duration)(f: => V)(implicit flags: Flags): V = {
        _cachingSync(keyParts: _*)(Some(ttl))(f)
      }

      /*
      Note: we could put our clever trousers on and abstract over synchronicity by saying
      `f` has to return an F[V] (where F is either a Future or an Id), but this would leak into the public API
      and probably confuse a lot of users.
       */
      private def _cachingSync(keyParts: Any*)(ttl: Option[Duration])(f: => V)(implicit flags: Flags): V = {
        val future = _caching(keyParts: _*)(ttl)(Future.successful(f))(flags, ExecutionContext.global)
        Await.result(future, Duration.Inf)
      }

      private def getSyncWithKey(key: String)(implicit flags: Flags): Option[V] =
        Await.result(getWithKey(key), Duration.Inf)

    }
  }

  /**
   * Create an instance of the 'typed' API that limits use of the cache to a single type.
   *
   * This means you can't accidentally put something of the wrong type into your cache,
   * and it reduces keystrokes because you don't need to specify types when calling `get`, `put`, etc.
   *
   * e.g. {{{
   * import scalacache._
   * implicit val sc: ScalaCache = ...
   *
   * val intCache = typed[Int]
   *
   * intCache.put("one")(1) // OK
   * intCache.put("two")(2.0) // Nope! Compiler error
   *
   * val one = intCache.get("1") // returns a Future[Option[Int]]
   * }}}
   *
   * @tparam V the type of values that the cache will accept
   */
  def typed[V, foo <: { type CodecTarget }](implicit sc: SingletonOf[ScalaCache, foo], scalaCache: ScalaCache, codec: Codec[V, foo#CodecTarget]) = new TypedApi[V, foo]()(sc, scalaCache, codec)

  /**
   * Get the value corresponding to the given key from the cache.
   *
   * Depending on the cache implementation, this may be done synchronously or asynchronously, so it returns a Future.
   *
   * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  def get[V, foo <: { type CodecTarget }](keyParts: Any*)(implicit sc: SingletonOf[ScalaCache, foo], scalaCache: ScalaCache, codec: Codec[V, foo#CodecTarget], flags: Flags): Future[Option[V]] =
    typed[V, foo].get(keyParts: _*)

//  /**
//   * Convenience method to get a value from the cache synchronously.
//   *
//   * Warning: may block indefinitely!
//   *
//   * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
//   * @tparam V the type of the corresponding value
//   * @return the value, if there is one
//   */
//  @deprecated("This method has moved. Please use scalacache.sync.get", "0.7.0")
//  def getSync[V](keyParts: Any*)(implicit scalaCache: ScalaCache, flags: Flags, codec: Codec[V]): Option[V] =
//    sync.get[V](keyParts: _*)

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
  def put[V, foo <: { type CodecTarget }](keyParts: Any*)(value: V, ttl: Option[Duration] = None)(implicit sc: SingletonOf[ScalaCache, foo], scalaCache: ScalaCache, codec: Codec[V, foo#CodecTarget], flags: Flags): Future[Unit] =
    typed[V, foo].put(keyParts: _*)(value, ttl)

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
   * Delete the entire contents of the cache. Use wisely!
   */
  def removeAll()(implicit scalaCache: ScalaCache): Future[Unit] =
    scalaCache.cache.removeAll()

  /**
   * Wrap the given block with a caching decorator.
   * First look in the cache. If the value is found, then return it immediately.
   * Otherwise run the block and save the result in the cache before returning it.
   *
   * All of the above happens asynchronously, so a `Future` is returned immediately.
   * Specifically:
   * - when the cache lookup completes, if it is a miss, the block execution is started.
   * - at some point after the block execution completes, the result is written asynchronously to the cache.
   * - the Future returned from this method does not wait for the cache write before completing.
   *
   * Note: Because no TTL is specified, the result will be stored in the cache indefinitely.
   *
   * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
   * @param f the block to run
   * @tparam V the type of the block's result
   * @return the result, either retrived from the cache or returned by the block
   */
  def caching[V, foo <: { type CodecTarget }](keyParts: Any*)(f: => Future[V])
                                             (implicit sc: SingletonOf[ScalaCache, foo], scalaCache: ScalaCache, flags: Flags, execContext: ExecutionContext = ExecutionContext.global, codec: Codec[V, foo#CodecTarget]): Future[V] =
    typed[V, foo].caching(keyParts: _*)(f)

  /**
   * Wrap the given block with a caching decorator.
   * First look in the cache. If the value is found, then return it immediately.
   * Otherwise run the block and save the result in the cache before returning it.
   *
   * All of the above happens asynchronously, so a `Future` is returned immediately.
   * Specifically:
   * - when the cache lookup completes, if it is a miss, the block execution is started.
   * - at some point after the block execution completes, the result is written asynchronously to the cache.
   * - the Future returned from this method does not wait for the cache write before completing.
   *
   * The result will be stored in the cache until the given TTL expires.
   *
   * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
   * @param ttl Time To Live
   * @param f the block to run
   * @tparam V the type of the block's result
   * @return the result, either retrived from the cache or returned by the block
   */
  def cachingWithTTL[V, foo <: { type CodecTarget }](keyParts: Any*)(ttl: Duration)(f: => Future[V])
                                                    (implicit sc: SingletonOf[ScalaCache, foo], scalaCache: ScalaCache, flags: Flags, execContext: ExecutionContext = ExecutionContext.global, codec: Codec[V, foo#CodecTarget]): Future[V] =
    typed[V, foo].cachingWithTTL(keyParts: _*)(ttl)(f)

  def cachingWithOptionalTTL[V, foo <: { type CodecTarget }](keyParts: Any*)(optionalTtl: Option[Duration])(f: => Future[V])
                                                            (implicit sc: SingletonOf[ScalaCache, foo], scalaCache: ScalaCache, flags: Flags, execContext: ExecutionContext = ExecutionContext.global, codec: Codec[V, foo#CodecTarget]): Future[V] =
    typed[V, foo].cachingWithOptionalTTL(keyParts: _*)(optionalTtl)(f)

  private def toKey(parts: Seq[Any])(implicit scalaCache: ScalaCache): String =
    scalaCache.keyBuilder.toCacheKey(parts)(scalaCache.cacheConfig)

//  /**
//   * Synchronous API, for the case when you don't want to deal with Futures.
//   */
//  object sync {
//
//    /**
//     * Convenience method to get a value from the cache synchronously.
//     *
//     * Warning: may block indefinitely!
//     *
//     * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
//     * @tparam V the type of the corresponding value
//     * @return the value, if there is one
//     */
//    def get[V](keyParts: Any*)(implicit scalaCache: ScalaCache, flags: Flags, codec: Codec[V]): Option[V] =
//      typed[V].sync.get(keyParts: _*)
//
//    /**
//     * Wrap the given block with a caching decorator.
//     * First look in the cache. If the value is found, then return it immediately.
//     * Otherwise run the block and save the result in the cache before returning it.
//     *
//     * Note: Because no TTL is specified, the result will be stored in the cache indefinitely.
//     *
//     * Warning: may block indefinitely!
//     *
//     * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
//     * @param f the block to run
//     * @tparam V the type of the block's result
//     * @return the result, either retrived from the cache or returned by the block
//     */
//    def caching[V](keyParts: Any*)(f: => V)(implicit scalaCache: ScalaCache, flags: Flags, codec: Codec[V]): V =
//      typed[V].sync.caching(keyParts: _*)(f)
//
//    /**
//     * Wrap the given block with a caching decorator.
//     * First look in the cache. If the value is found, then return it immediately.
//     * Otherwise run the block and save the result in the cache before returning it.
//     *
//     * The result will be stored in the cache until the given TTL expires.
//     *
//     * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
//     * @param ttl Time To Live
//     * @param f the block to run
//     * @tparam V the type of the block's result
//     * @return the result, either retrived from the cache or returned by the block
//     */
//    def cachingWithTTL[V](keyParts: Any*)(ttl: Duration)(f: => V)(implicit scalaCache: ScalaCache, flags: Flags, codec: Codec[V]): V =
//      typed[V].sync.cachingWithTTL(keyParts: _*)(ttl)(f)
//
//    /**
//     * Wrap the given block with a caching decorator.
//     * First look in the cache. If the value is found, then return it immediately.
//     * Otherwise run the block and save the result in the cache before returning it.
//     *
//     * The result will be stored in the cache until the given TTL expires.
//     *
//     * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
//     * @param optionalTtl Optional Time To Live
//     * @param f the block to run
//     * @tparam V the type of the block's result
//     * @return the result, either retrived from the cache or returned by the block
//     */
//    def cachingWithOptionalTTL[V](keyParts: Any*)(optionalTtl: Option[Duration])(f: => V)(implicit scalaCache: ScalaCache, flags: Flags, codec: Codec[V]): V = {
//      optionalTtl match {
//        case Some(ttl) => typed[V].sync.cachingWithTTL(keyParts: _*)(ttl)(f)
//        case None => typed[V].sync.caching(keyParts: _*)(f)
//      }
//    }
//
//  }

}
