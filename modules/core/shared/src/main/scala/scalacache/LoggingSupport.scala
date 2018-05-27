package scalacache

import org.slf4j.Logger

import scala.concurrent.duration.Duration

/**
  * Helper methods for logging
  */
trait LoggingSupport {

  protected val logger: Logger

  /**
    * Output a debug log to record the result of a cache lookup
    *
    * @param key the key that was looked up
    * @param result the result of the cache lookup
    * @tparam A the type of the cache value
    */
  @inline protected final def logCacheHitOrMiss[A](key: String, result: Option[A]): Unit =
    if (logger.isDebugEnabled) {
      val hitOrMiss = result.fold("miss")(_ => "hit")
      logger.debug(s"Cache $hitOrMiss for key $key")
    }

  /**
    * Output a debug log to record a cache insertion/update
    *
    * @param key the key that was inserted/updated
    * @param ttl the TTL of the inserted entry
    */

  @inline protected final def logCachePut(key: String, ttl: Option[Duration]): Unit =
    if (logger.isDebugEnabled) {
      val ttlMsg = ttl.fold("")(d => s" with TTL ${d.toMillis} ms")
      logger.debug(s"Inserted value into cache with key $key$ttlMsg")
    }

}
