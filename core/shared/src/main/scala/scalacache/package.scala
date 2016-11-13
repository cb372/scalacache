
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.language.higherKinds
import scalacache.serialization.{ Codec, JavaSerializationCodec }

// TODO review all Scaladocs and rewrite as necessary. A lot of them mention Future.

package object scalacache extends JavaSerializationCodec {
  private final val logger = LoggerFactory.getLogger(getClass.getName)

  // this alias is just for convenience, so you don't need to import serialization._
  type NoSerialization = scalacache.serialization.InMemoryRepr

  // TODO move codec out of constructor and into each method? Repr would become a type member of ScalaCache instead of a type param.
  // This would mean users can write `typed[String]` instead of `typed[String, NoSerialization]`.

  class TypedApi[From, Repr](implicit val scalaCache: ScalaCache[Repr], codec: Codec[From, Repr]) { self =>

    def get(keyParts: Any*)(implicit flags: Flags, mode: Mode): mode.F[Option[From]] =
      getWithKey(toKey(keyParts))(flags, mode)

    def put(keyParts: Any*)(value: From, ttl: Option[Duration] = None)(implicit flags: Flags, mode: Mode): mode.F[Unit] =
      putWithKey(toKey(keyParts), value, ttl)(flags, mode)

    def remove(keyParts: Any*)(implicit mode: Mode): mode.F[Unit] =
      scalacache.remove(keyParts: _*)

    def removeAll(implicit mode: Mode): mode.F[Unit] =
      scalacache.removeAll()

    def caching[M[_]](keyParts: Any*)(ttl: Option[Duration])(f: => M[From])(implicit flags: Flags, mode: Mode.Aux[M]): M[From] = {
      _caching[M](keyParts: _*)(ttl)(f)
    }

    // TODO
    //    private[scalacache] def cachingForMemoize(baseKey: String)(ttl: Option[Duration])(f: => Future[From])(implicit flags: Flags, execContext: ExecutionContext): Future[From] = {
    //      val key = stringToKey(baseKey)
    //      _caching(key)(ttl)(f)
    //    }

    private def _caching[M[_]](keyParts: Any*)(ttl: Option[Duration])(f: => M[From])(implicit flags: Flags, mode: Mode.Aux[M]): M[From] = {
      val key = toKey(keyParts)
      _caching[M](key)(ttl)(f)(flags, mode)
    }

    private def _caching[M[_]](key: String)(ttl: Option[Duration])(f: => M[From])(implicit flags: Flags, mode: Mode.Aux[M]): M[From] = {

      def cacheAndReturn(): M[From] = {
        val m: M[From] = f
        mode.flatMap(m) { (value: From) =>
          val put: M[Unit] = putWithKey(key, value, ttl)
          mode.map(put) { _ =>
            value
          }
        }
      }

      mode.flatMap(getWithKey(key)) { (option: Option[From]) =>
        option match {
          case Some(value) => mode.wrap[From](value)
          case None => cacheAndReturn()
        }
      }
    }

    private def getWithKey(key: String)(implicit flags: Flags, mode: Mode): mode.F[Option[From]] = {
      if (flags.readsEnabled) {
        mode.wrap(scalaCache.cache.get[From](key))
      } else {
        if (logger.isDebugEnabled) {
          logger.debug(s"Skipping cache GET because cache reads are disabled. Key: $key")
        }
        mode.wrap(None)
      }
    }

    private def putWithKey(key: String, value: From, ttl: Option[Duration] = None)(implicit flags: Flags, mode: Mode): mode.F[Unit] = {
      if (flags.writesEnabled) {
        val finiteTtl = ttl.filter(_.isFinite()) // discard Duration.Inf, Duration.Undefined
        mode.wrap(scalaCache.cache.put(key, value, finiteTtl))
      } else {
        if (logger.isDebugEnabled) {
          logger.debug(s"Skipping cache PUT because cache writes are disabled. Key: $key")
        }
        mode.wrap(())
      }
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
  def typed[V, Repr](implicit scalaCache: ScalaCache[Repr], codec: Codec[V, Repr]) = new TypedApi[V, Repr]()(scalaCache, codec)

  // TODO get rid of these and just use the typed API? They're not very convenient because type inference doesn't work well enough

  /**
   * Get the value corresponding to the given key from the cache.
   *
   * Depending on the cache implementation, this may be done synchronously or asynchronously, so it returns a Future.
   *
   * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  def get[V, Repr](keyParts: Any*)(implicit scalaCache: ScalaCache[Repr], flags: Flags, codec: Codec[V, Repr], mode: Mode): mode.F[Option[V]] =
    typed[V, Repr].get(keyParts: _*)

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
  def put[V, Repr](keyParts: Any*)(value: V, ttl: Option[Duration] = None)(implicit scalaCache: ScalaCache[Repr], flags: Flags, codec: Codec[V, Repr], mode: Mode): mode.F[Unit] =
    typed[V, Repr].put(keyParts: _*)(value, ttl)

  /**
   * Remove the given key and its associated value from the cache, if it exists.
   * If the key is not in the cache, do nothing.
   *
   * Depending on the cache implementation, this may be done synchronously or asynchronously, so it returns a Future.
   *
   * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
   */
  def remove(keyParts: Any*)(implicit scalaCache: ScalaCache[_], mode: Mode): mode.F[Unit] =
    mode.wrap(scalaCache.cache.remove(toKey(keyParts)))

  /**
   * Delete the entire contents of the cache. Use wisely!
   */
  def removeAll()(implicit scalaCache: ScalaCache[_], mode: Mode): mode.F[Unit] =
    mode.wrap(scalaCache.cache.removeAll())

  /**
   *
   * // TODO rewrite this comment
   *
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
  def caching[V, Repr, M[_]](keyParts: Any*)(ttl: Option[Duration])(f: => M[V])(implicit scalaCache: ScalaCache[Repr], flags: Flags, codec: Codec[V, Repr], mode: Mode.Aux[M]): M[V] =
    typed[V, Repr].caching(keyParts: _*)(ttl)(f)

  // TODO
  //  // Note: this is public because the macro inserts a call to this method into client code
  //  def cachingForMemoize[V, Repr](key: String)(optionalTtl: Option[Duration])(f: => Future[V])(implicit scalaCache: ScalaCache[Repr], flags: Flags, execContext: ExecutionContext = ExecutionContext.global, codec: Codec[V, Repr]): Future[V] =
  //    typed[V, Repr].cachingForMemoize(key)(optionalTtl)(f)

  private def toKey(parts: Seq[Any])(implicit scalaCache: ScalaCache[_]): String =
    scalaCache.keyBuilder.toCacheKey(parts)(scalaCache.cacheConfig)

  private def stringToKey(string: String)(implicit scalaCache: ScalaCache[_]): String =
    scalaCache.keyBuilder.stringToCacheKey(string)(scalaCache.cacheConfig)

}
