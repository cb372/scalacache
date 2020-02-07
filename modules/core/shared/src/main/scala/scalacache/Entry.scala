package scalacache

import java.time.Instant
import cats.effect.Clock
import java.util.concurrent.TimeUnit
import cats.implicits._
import language.higherKinds
import cats.Applicative

/**
  * A cache entry with an optional expiry time
  */
case class Entry[F[_], +A](value: A, expiresAt: Option[Instant]) {

  /**
    * Has the entry expired yet?
    */
  def isExpired(implicit clock: Clock[F], applicative: Applicative[F]): F[Boolean] =
    expiresAt
      .traverse { ttl =>
        clock.monotonic(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli(_)).map(ttl.isBefore(_))
      }
      .map(_.contains(true))
}
