package scalacache

import scala.concurrent.duration.Duration

import scala.language.higherKinds
import cats.Monad
import cats.syntax.all._
import cats.MonadError
import cats.effect.Sync
import cats.Applicative

/** Extension methods for an AbstractCache. *
  */
class AbstractCacheOps[F[_]: Monad, V](underlying: AbstractCache[F, V]) {

  /** Get a value from the cache if it exists. Otherwise compute it, insert it into the cache if the computed value is
    * Right, and return it.
    *
    * @param keyParts
    *   The cache key
    * @param ttl
    *   The time-to-live to use when inserting into the cache. The cache entry will expire after this time has elapsed.
    * @param f
    *   A block that computes the Either of value
    * @param flags
    *   Flags used to conditionally alter the behaviour of ScalaCache
    * @return
    *   The value, either retrieved from the cache or computed
    */
  def cachingRight[A](
      keyParts: Any*
  )(
      ttl: Option[Duration] = None
  )(
      f: => Either[A, V]
  )(implicit flags: Flags): F[Either[A, V]] = {
    underlying.get(keyParts: _*).flatMap {
      case None =>
        f match {
          case Right(v) => underlying.put(keyParts: _*)(v, ttl).map(_ => v.asRight[A])
          case Left(a)  => a.asLeft[V].pure
        }
      case Some(v) => v.asRight[A].pure
    }
  }

  /** Get a value from the cache if it exists. Otherwise compute it, insert it into the cache if the computed value is
    * Some, and return it.
    *
    * @param keyParts
    *   The cache key
    * @param ttl
    *   The time-to-live to use when inserting into the cache. The cache entry will expire after this time has elapsed.
    * @param f
    *   A block that computes the optional value
    * @param flags
    *   Flags used to conditionally alter the behaviour of ScalaCache
    * @return
    *   The value, either retrieved from the cache or computed
    */
  def cachingSome(
      keyParts: Any*
  )(
      ttl: Option[Duration] = None
  )(
      f: => Option[V]
  )(implicit flags: Flags): F[Option[V]] = {
    underlying.get(keyParts: _*).flatMap {
      case None =>
        f match {
          case Some(v) => underlying.put(keyParts: _*)(v, ttl).map(_ => v.some)
          case None    => Option.empty[V].pure
        }
      case Some(v) => v.some.pure
    }
  }

  /** Get a value from the cache if it exists. Otherwise compute it, insert it into the cache if the computed value is
    * Right, and return it.
    *
    * @param keyParts
    *   The cache key
    * @param ttl
    *   The time-to-live to use when inserting into the cache. The cache entry will expire after this time has elapsed.
    * @param f
    *   A block that computes the Either of value wrapped in a container
    * @param flags
    *   Flags used to conditionally alter the behaviour of ScalaCache
    * @return
    *   The value, either retrieved from the cache or computed
    */
  def cachingRightF[A](
      keyParts: Any*
  )(
      ttl: Option[Duration] = None
  )(
      f: F[Either[A, V]]
  )(implicit flags: Flags): F[Either[A, V]] = {
    underlying.get(keyParts: _*).flatMap {
      case None =>
        f.flatMap {
          case Right(v) => underlying.put(keyParts: _*)(v, ttl).map(_ => v.asRight[A])
          case Left(a)  => a.asLeft[V].pure
        }
      case Some(v) => v.asRight[A].pure
    }
  }

  /** Get a value from the cache if it exists. Otherwise compute it, insert it into the cache if the computed value is
    * Some, and return it.
    *
    * @param keyParts
    *   The cache key
    * @param ttl
    *   The time-to-live to use when inserting into the cache. The cache entry will expire after this time has elapsed.
    * @param f
    *   A block that computes the optional value wrapped in a container
    * @param flags
    *   Flags used to conditionally alter the behaviour of ScalaCache
    * @return
    *   The value, either retrieved from the cache or computed
    */
  def cachingSomeF(
      keyParts: Any*
  )(
      ttl: Option[Duration] = None
  )(
      f: F[Option[V]]
  )(implicit flags: Flags): F[Option[V]] = {
    underlying.get(keyParts: _*).flatMap {
      case None =>
        f.flatMap {
          case Some(v) => underlying.put(keyParts: _*)(v, ttl).map(_ => v.some)
          case None    => Option.empty[V].pure
        }
      case Some(v) => v.some.pure
    }
  }

}
