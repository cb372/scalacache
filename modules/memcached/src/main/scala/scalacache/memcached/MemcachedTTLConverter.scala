/*
 * Copyright 2021 scalacache
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package scalacache.memcached

import java.time.{Clock, Instant}

import org.slf4j.LoggerFactory

import scala.concurrent.duration._

trait MemcachedTTLConverter {
  private final val logger = LoggerFactory.getLogger(getClass.getName)

  /** Convert an optional `Duration` to an int suitable for passing to Memcached.
    *
    * From the Memcached protocol spec:
    *
    * <blockquote> <p> The actual value sent may either be Unix time (number of seconds since January 1, 1970, as a
    * 32-bit value), or a number of seconds starting from current time. In the latter case, this number of seconds may
    * not exceed 60*60*24*30 (number of seconds in 30 days); if the number sent by a client is larger than that, the
    * server will consider it to be real Unix time value rather than an offset from current time. </p> </blockquote>
    *
    * @param ttl
    *   optional TTL
    * @return
    *   corresponding Memcached expiry
    */
  def toMemcachedExpiry(ttl: Option[Duration])(implicit clock: Clock = Clock.systemUTC()): Int = {
    ttl.map(durationToExpiry).getOrElse(0)
  }

  private def durationToExpiry(duration: Duration)(implicit clock: Clock): Int = duration match {
    case Duration.Zero => 0

    case d if d < 1.second => {
      if (logger.isWarnEnabled) {
        logger.warn(s"Because Memcached does not support sub-second expiry, TTL of $d will be rounded up to 1 second")
      }
      1
    }

    case d if d <= 30.days => d.toSeconds.toInt

    case d => {
      val expiryTime = Instant.now(clock).plusSeconds(d.toSeconds.toLong)
      (expiryTime.toEpochMilli / 1000).toInt
    }
  }

}
