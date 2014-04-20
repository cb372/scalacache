package scalacache

import scala.concurrent.duration.Duration
import com.typesafe.scalalogging.slf4j.StrictLogging

/**
 * Helper methods for logging
 *
 * Author: chris
 * Created: 4/9/14
 */
trait LoggingSupport { self: StrictLogging =>

  /**
   * Output a debug log to record the result of a cache lookup
   *
   * @param key the key that was looked up
   * @param result the result of the cache lookup
   * @tparam A the type of the cache value
   */
  protected def logCacheHitOrMiss[A](key: String, result: Option[A]): Unit = {
    val hitOrMiss = result.map(_ => "hit") getOrElse "miss"
    logger.debug(s"Cache $hitOrMiss for key $key")
  }

  /**
   * Output a debug log to record a cache insertion/update
   *
   * @param key the key that was inserted/updated
   * @param ttl the TTL of the inserted entry
   */
  protected def logCachePut(key: String, ttl: Option[Duration]): Unit = {
    val ttlMsg = ttl.map(d => s" with TTL ${d.toMillis} ms") getOrElse ""
    logger.debug(s"Inserted value into cache with key $key$ttlMsg")
  }

}
