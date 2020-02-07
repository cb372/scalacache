package scalacache

import java.time.{Instant}
import cats.effect.Clock
import java.util.concurrent.TimeUnit
import cats.implicits._
import language.higherKinds
import cats.Functor

/**
  * A cache entry with an optional expiry time
  */
case class Entry[F[_], +A](value: A, expiresAt: Option[Instant]) {

  /**
    * Has the entry expired yet?
    */
  def isExpired(implicit clock: Clock[F], functor: Functor[F]): F[Boolean] =
    clock.monotonic(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli(_)).map(now => expiresAt.exists(_.isBefore(now)))
}
