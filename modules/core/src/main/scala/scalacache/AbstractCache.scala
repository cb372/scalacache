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

import cats.effect.Sync
import cats.implicits._

import scala.concurrent.duration.Duration

/** An abstract implementation of [[Cache]] that takes care of some things that are common across all concrete
  * implementations.
  *
  * If you are writing a cache implementation, you probably want to extend this trait rather than extending [[Cache]]
  * directly.
  *
  * @tparam K
  *   The type of keys stored in the cache.
  * @tparam V
  *   The type of values stored in the cache.
  */
trait AbstractCache[F[_], K, V] extends Cache[F, K, V] with LoggingSupport[F, K] {

  protected implicit def F: Sync[F]
  // GET

  protected def doGet(key: K): F[Option[V]]

  private def checkFlagsAndGet(key: K)(implicit flags: Flags): F[Option[V]] = {
    if (flags.readsEnabled) {
      doGet(key)
    } else
      logger
        .ifDebugEnabled {
          logger.debug(s"Skipping cache GET because cache reads are disabled. Key: $key")
        }
        .as(None)
  }

  final override def get(key: K)(implicit flags: Flags): F[Option[V]] = {
    checkFlagsAndGet(key)
  }

  // PUT

  protected def doPut(key: K, value: V, ttl: Option[Duration]): F[Unit]

  private def checkFlagsAndPut(key: K, value: V, ttl: Option[Duration])(implicit
      flags: Flags
  ): F[Unit] = {
    if (flags.writesEnabled) {
      doPut(key, value, ttl)
    } else
      logger.ifDebugEnabled {
        logger.debug(s"Skipping cache PUT because cache writes are disabled. Key: $key")
      }.void
  }

  final override def put(
      key: K
  )(value: V, ttl: Option[Duration])(implicit flags: Flags): F[Unit] = {
    val finiteTtl = ttl.filter(_.isFinite) // discard Duration.Inf, Duration.Undefined
    checkFlagsAndPut(key, value, finiteTtl)
  }

  // REMOVE

  protected def doRemove(key: K): F[Unit]

  final override def remove(key: K): F[Unit] =
    doRemove(key)

  // REMOVE ALL

  protected def doRemoveAll: F[Unit]

  final override def removeAll: F[Unit] =
    doRemoveAll

  // CACHING

  final override def caching(
      key: K
  )(ttl: Option[Duration] = None)(f: => V)(implicit flags: Flags): F[V] = cachingF(key)(ttl)(Sync[F].delay(f))

  override def cachingF(
      key: K
  )(ttl: Option[Duration] = None)(f: F[V])(implicit flags: Flags): F[V] = {
    checkFlagsAndGet(key)
      .handleErrorWith { e =>
        logger
          .ifWarnEnabled(logger.warn(s"Failed to read from cache. Key = $key", e))
          .as(None)
      }
      .flatMap {
        case Some(valueFromCache) => F.pure(valueFromCache)
        case None =>
          f.flatTap { calculatedValue =>
            checkFlagsAndPut(key, calculatedValue, ttl)
              .handleErrorWith { e =>
                logger.ifWarnEnabled {
                  logger.warn(s"Failed to write to cache. Key = $key", e)
                }.void
              }
          }
      }
  }
}
