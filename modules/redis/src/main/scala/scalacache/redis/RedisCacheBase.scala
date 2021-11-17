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

package scalacache.redis

import redis.clients.jedis.commands.BinaryJedisCommands
import redis.clients.jedis.util.Pool

import scalacache.logging.Logger
import scalacache.serialization.Codec
import scalacache.serialization.binary.{BinaryCodec, BinaryEncoder}

import java.io.Closeable
import scala.concurrent.duration._
import cats.effect.Resource
import cats.syntax.all._
import scalacache.AbstractCache

/** Contains implementations of all methods that can be implemented independent of the type of Redis client. This is
  * everything apart from `removeAll`, which needs to be implemented differently for sharded Redis.
  */
trait RedisCacheBase[F[_], K, V] extends AbstractCache[F, K, V] {

  override protected final val logger = Logger.getLogger[F](getClass.getName)

  protected type JClient <: BinaryJedisCommands with Closeable

  protected def jedisPool: Pool[JClient]

  protected def keyEncoder: BinaryEncoder[K]
  protected def codec: BinaryCodec[V]

  protected def doGet(key: K): F[Option[V]] =
    withJedis { jedis =>
      val bytes = jedis.get(keyEncoder.encode(key))
      val result: Codec.DecodingResult[Option[V]] = {
        if (bytes != null)
          codec.decode(bytes).map(Some(_))
        else
          Right(None)
      }

      result match {
        case Left(e) =>
          F.raiseError[Option[V]](e)
        case Right(maybeValue) =>
          logCacheHitOrMiss(key, maybeValue).as(maybeValue)
      }
    }

  protected def doPut(key: K, value: V, ttl: Option[Duration]): F[Unit] = {
    withJedis { jedis =>
      val keyBytes   = keyEncoder.encode(key)
      val valueBytes = codec.encode(value)
      ttl match {
        case None                => F.delay(jedis.set(keyBytes, valueBytes))
        case Some(Duration.Zero) => F.delay(jedis.set(keyBytes, valueBytes))
        case Some(d) if d < 1.second =>
          logger.ifWarnEnabled {
            logger.warn(
              s"Because Redis (pre 2.6.12) does not support sub-second expiry, TTL of $d will be rounded up to 1 second"
            )
          } *> F.delay {
            jedis.setex(keyBytes, 1L, valueBytes)
          }
        case Some(d) =>
          F.delay(jedis.setex(keyBytes, d.toSeconds.toLong, valueBytes))
      }
    } *> logCachePut(key, ttl)
  }

  protected def doRemove(key: K): F[Unit] = {
    withJedis { jedis =>
      F.delay(jedis.del(keyEncoder.encode(key))).void
    }
  }

  val close: F[Unit] = F.delay(jedisPool.close())

  /** Borrow a Jedis client from the pool, perform some operation and then return the client to the pool.
    *
    * @param f
    *   block that uses the Jedis client.
    * @tparam T
    *   return type of the block
    * @return
    *   the result of executing the block
    */
  protected final def withJedis[T](f: JClient => F[T]): F[T] = {
    Resource.fromAutoCloseable(F.delay(jedisPool.getResource())).use(jedis => F.defer(f(jedis)))
  }

}
