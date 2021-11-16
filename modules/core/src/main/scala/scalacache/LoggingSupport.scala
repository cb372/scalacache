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

package scalacache

import scalacache.logging.Logger

import scala.concurrent.duration.Duration
import cats.Monad
import cats.implicits._

/** Helper methods for logging
  */
trait LoggingSupport[F[_], K] {
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
  protected def logCacheHitOrMiss[A](key: K, result: Option[A]): F[Unit] =
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
  protected def logCachePut(key: K, ttl: Option[Duration]): F[Unit] =
    logger.ifDebugEnabled {
      val ttlMsg = ttl.map(d => s" with TTL ${d.toMillis} ms") getOrElse ""
      logger.debug(s"Inserted value into cache with key $key$ttlMsg")
    }.void
}
