
import cats.{ Id, Monad }
import cats.arrow.FunctionK
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.language.higherKinds
import scala.util.{ Failure, Success, Try }
import scalacache.serialization.{ Codec, JavaSerializationCodec }

package object scalacache extends JavaSerializationCodec {
  private final val logger = LoggerFactory.getLogger(getClass.getName)

  implicit val futureToId = new FunctionK[Future, Id] {
    override def apply[A](fa: Future[A]): Id[A] = Await.result(fa, atMost = Duration.Inf)
  }
  implicit val idToFuture = new FunctionK[Id, Future] {
    override def apply[A](a: Id[A]): Future[A] = Future.successful(a)
  }
  implicit val idToId = new FunctionK[Id, Id] {
    override def apply[A](a: Id[A]): Id[A] = a
  }
  implicit val futureToFuture = new FunctionK[Future, Future] {
    override def apply[A](fa: Future[A]): Future[A] = fa
  }

  // this alias is just for convenience, so you don't need to import serialization._
  type NoSerialization = scalacache.serialization.InMemoryRepr

  class TypedApi[From, Repr, F[_]: Monad](implicit val scalaCache: ScalaCache[Repr, F], codec: Codec[From, Repr]) { self =>

    def get[G[_]: Monad](keyParts: Any*)(implicit flags: Flags, transform: FunctionK[F, G]): G[Option[From]] =
      getWithKey[G](toKey(keyParts))

    def put[G[_]: Monad](keyParts: Any*)(value: From, ttl: Option[Duration] = None)(implicit flags: Flags, transform: FunctionK[F, G]): G[Unit] =
      putWithKey[G](toKey(keyParts), value, ttl)

    def remove[G[_]: Monad](keyParts: Any*)(implicit transform: FunctionK[F, G]): G[Unit] =
      scalacache.remove[F, G](keyParts: _*)

    def removeAll[G[_]: Monad]()(implicit transform: FunctionK[F, G]): G[Unit] =
      scalacache.removeAll[F, G]()

    def caching[G[_]: Monad](keyParts: Any*)(optionalTtl: Option[Duration])(f: => From)(implicit flags: Flags, execContext: ExecutionContext = ExecutionContext.global, transform: FunctionK[F, G]): G[From] = {
      _caching[G](keyParts: _*)(optionalTtl)(f)
    }

    //    private[scalacache] def cachingForMemoize(baseKey: String)(ttl: Option[Duration])(f: => Future[From])(implicit flags: Flags, execContext: ExecutionContext): Future[From] = {
    //      val key = stringToKey(baseKey)
    //      _caching(key)(ttl)(f)
    //    }

    private def _caching[G[_]: Monad](keyParts: Any*)(ttl: Option[Duration])(f: => From)(implicit flags: Flags, execContext: ExecutionContext, transform: FunctionK[F, G]): G[From] = {
      val key = toKey(keyParts)
      _caching[G](key)(ttl)(f)
    }

    private def _caching[G[_]: Monad](key: String)(ttl: Option[Duration])(f: => From)(implicit flags: Flags, execContext: ExecutionContext, transform: FunctionK[F, G]): G[From] = {

      def calculateAndCache(): From = {
        val calculatedValue = f
        Try(putWithKey[G](key, calculatedValue, ttl)) recover {
          case e =>
            if (logger.isWarnEnabled) {
              logger.warn(s"Failed to write to cache. Key = $key", e)
            }
        }
        calculatedValue
      }

      import cats.syntax.functor._
      getWithKey[G](key).map(fromCache => fromCache getOrElse calculateAndCache())
    }

    private def getWithKey[G[_]: Monad](key: String)(implicit flags: Flags, transform: FunctionK[F, G]): G[Option[From]] = {
      if (flags.readsEnabled) {
        val result: F[Option[From]] = scalaCache.cache.get[From](key)
        transform(result)
      } else {
        if (logger.isDebugEnabled) {
          logger.debug(s"Skipping cache GET because cache reads are disabled. Key: $key")
        }
        Monad[G].pure(None)
      }
    }

    private def putWithKey[G[_]: Monad](key: String, value: From, ttl: Option[Duration] = None)(implicit flags: Flags, transform: FunctionK[F, G]): G[Unit] = {
      if (flags.writesEnabled) {
        val finiteTtl = ttl.filter(_.isFinite()) // discard Duration.Inf, Duration.Undefined
        val result: F[Unit] = scalaCache.cache.put(key, value, finiteTtl)
        transform(result)
      } else {
        if (logger.isDebugEnabled) {
          logger.debug(s"Skipping cache PUT because cache writes are disabled. Key: $key")
        }
        Monad[G].pure(())
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
  def typed[V, Repr, F[_]](implicit mf: Monad[F], scalaCache: ScalaCache[Repr, F], codec: Codec[V, Repr]) =
    new TypedApi[V, Repr, F]()(mf, scalaCache, codec)

  /**
   * Get the value corresponding to the given key from the cache.
   *
   * Depending on the cache implementation, this may be done synchronously or asynchronously, so it returns a Future.
   *
   * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  def get[V, Repr, F[_]: Monad, G[_]: Monad](keyParts: Any*)(implicit scalaCache: ScalaCache[Repr, F], flags: Flags, codec: Codec[V, Repr], transform: FunctionK[F, G]): G[Option[V]] =
    typed[V, Repr, F].get[G](keyParts: _*)

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
  def put[V, Repr, F[_]: Monad, G[_]: Monad](keyParts: Any*)(value: V, ttl: Option[Duration] = None)(implicit scalaCache: ScalaCache[Repr, F], flags: Flags, codec: Codec[V, Repr], transform: FunctionK[F, G]): G[Unit] =
    typed[V, Repr, F].put[G](keyParts: _*)(value, ttl)

  /**
   * Remove the given key and its associated value from the cache, if it exists.
   * If the key is not in the cache, do nothing.
   *
   * Depending on the cache implementation, this may be done synchronously or asynchronously, so it returns a Future.
   *
   * @param keyParts data to be used to generate the cache key. This could be as simple as just a single String. See [[CacheKeyBuilder]].
   */
  def remove[F[_]: Monad, G[_]: Monad](keyParts: Any*)(implicit scalaCache: ScalaCache[_, F], transform: FunctionK[F, G]): G[Unit] =
    transform(scalaCache.cache.remove(toKey(keyParts)))

  /**
   * Delete the entire contents of the cache. Use wisely!
   */
  def removeAll[F[_]: Monad, G[_]: Monad]()(implicit scalaCache: ScalaCache[_, F], transform: FunctionK[F, G]): G[Unit] =
    transform(scalaCache.cache.removeAll())

  def caching[V, Repr, F[_]: Monad, G[_]: Monad](keyParts: Any*)(optionalTtl: Option[Duration])(f: => V)(implicit scalaCache: ScalaCache[Repr, F], flags: Flags, execContext: ExecutionContext = ExecutionContext.global, codec: Codec[V, Repr], transform: FunctionK[F, G]): G[V] =
    typed[V, Repr, F].caching[G](keyParts: _*)(optionalTtl)(f)

  //  // Note: this is public because the macro inserts a call to this method into client code
  //  def cachingForMemoize[V, Repr](key: String)(optionalTtl: Option[Duration])(f: => Future[V])(implicit scalaCache: ScalaCache[Repr], flags: Flags, execContext: ExecutionContext = ExecutionContext.global, codec: Codec[V, Repr]): Future[V] =
  //    typed[V, Repr].cachingForMemoize(key)(optionalTtl)(f)

  private def toKey[X[_]](parts: Seq[Any])(implicit scalaCache: ScalaCache[_, X]): String =
    scalaCache.keyBuilder.toCacheKey(parts)(scalaCache.cacheConfig)

  private def stringToKey[X[_]](string: String)(implicit scalaCache: ScalaCache[_, X]): String =
    scalaCache.keyBuilder.stringToCacheKey(string)(scalaCache.cacheConfig)

}
