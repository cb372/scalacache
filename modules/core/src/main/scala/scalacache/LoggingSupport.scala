package scalacache

import scalacache.logging.Logger

import scala.concurrent.duration.Duration
import cats.effect.Sync
import cats.Applicative
import cats.Monad
import cats.implicits._

/** Helper methods for logging
  */
trait LoggingSupport[F[_]] {
  protected def logger: Logger[F]
  protected implicit def F: Monad[F]

  /** Output a debug log to record the result of a cache lookup
    *
    * @param key
    *   the key that was looked up
    * @param result
    *   the result of the cache lookup
    * @tparam A
    *   the type of the cache value
    */
  protected def logCacheHitOrMiss[A](key: String, result: Option[A]): F[Unit] =
    logger.ifDebugEnabled {
      val hitOrMiss = result.map(_ => "hit") getOrElse "miss"
      logger.debug(s"Cache $hitOrMiss for key $key")
    }.void

  /** Output a debug log to record a cache insertion/update
    *
    * @param key
    *   the key that was inserted/updated
    * @param ttl
    *   the TTL of the inserted entry
    */
  protected def logCachePut(key: String, ttl: Option[Duration]): F[Unit] =
    logger.ifDebugEnabled {
      val ttlMsg = ttl.map(d => s" with TTL ${d.toMillis} ms") getOrElse ""
      logger.debug(s"Inserted value into cache with key $key$ttlMsg")
    }.void
}
