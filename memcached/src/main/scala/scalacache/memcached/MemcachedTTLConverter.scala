package scalacache.memcached

import org.slf4j.LoggerFactory
import org.joda.time.DateTime

import scala.concurrent.duration._

trait MemcachedTTLConverter {
  private final val logger = LoggerFactory.getLogger(getClass.getName)

  /**
   * Convert an optional `Duration` to an int suitable for passing to Memcached.
   *
   * From the Memcached protocol spec:
   *
   * <blockquote>
   * <p>
   * The actual value sent may either be Unix time (number of seconds since
   * January 1, 1970, as a 32-bit value), or a number of seconds starting from
   * current time. In the latter case, this number of seconds may not exceed
   * 60*60*24*30 (number of seconds in 30 days); if the number sent by a client
   * is larger than that, the server will consider it to be real Unix time value
   * rather than an offset from current time.
   * </p>
   * </blockquote>
   *
   * @param ttl optional TTL
   * @return corresponding Memcached expiry
   */
  def toMemcachedExpiry(ttl: Option[Duration]): Int = {
    ttl.map(durationToExpiry).getOrElse(0)
  }

  private def durationToExpiry(duration: Duration): Int = duration match {
    case Duration.Zero => 0

    case d if d < 1.second => {
      if (logger.isWarnEnabled) {
        logger.warn(s"Because Memcached does not support sub-second expiry, TTL of $d will be rounded up to 1 second")
      }
      1
    }

    case d if d <= 30.days => d.toSeconds.toInt

    case d => {
      val expiryTime = DateTime.now.plusSeconds(d.toSeconds.toInt)
      (expiryTime.getMillis / 1000).toInt
    }
  }

}
