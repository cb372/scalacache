package scalacache

import java.time.Instant
import cats.effect.Clock
import java.util.concurrent.TimeUnit
import cats.implicits._
import language.higherKinds
import cats.Applicative

/** A cache entry with an optional expiry time
  */
case class Entry[+A](value: A, expiresAt: Option[Instant])

object Entry {

  def isBeforeExpiration[F[_], A](entry: Entry[A])(implicit clock: Clock[F], applicative: Applicative[F]): F[Boolean] =
    entry.expiresAt
      .traverse { expiration =>
        clock.monotonic.map(m => Instant.ofEpochMilli(m.toMillis).isBefore(expiration))
      }
      .map {
        case None                   => true // no expiration set for entry, never expires
        case Some(beforeExpiration) => beforeExpiration
      }

  def isExpired[F[_], A](entry: Entry[A])(implicit clock: Clock[F], applicative: Applicative[F]): F[Boolean] =
    isBeforeExpiration[F, A](entry).map(b => !b)
}
