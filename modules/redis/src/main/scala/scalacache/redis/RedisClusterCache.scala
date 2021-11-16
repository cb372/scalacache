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

import scalacache.logging.Logger
import scalacache.serialization.Codec
import scalacache.serialization.binary.{BinaryCodec, BinaryEncoder}

import scala.concurrent.duration.{Duration, _}
import cats.implicits._
import cats.effect.Sync
import redis.clients.jedis.JedisCluster
import redis.clients.jedis.exceptions.JedisClusterException
import scalacache.AbstractCache

class RedisClusterCache[F[_]: Sync, K, V](val jedisCluster: JedisCluster)(implicit
    val keyEncoder: BinaryEncoder[K],
    val codec: BinaryCodec[V]
) extends AbstractCache[F, K, V] {

  protected def F: Sync[F] = Sync[F]

  override protected final val logger = Logger.getLogger(getClass.getName)

  override protected def doGet(key: K): F[Option[V]] = F.defer {
    val bytes = jedisCluster.get(keyEncoder.encode(key))
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

  override protected def doPut(key: K, value: V, ttl: Option[Duration]): F[Unit] = {
    val keyBytes   = keyEncoder.encode(key)
    val valueBytes = codec.encode(value)
    ttl match {
      case None                => F.delay(jedisCluster.set(keyBytes, valueBytes)).void
      case Some(Duration.Zero) => F.delay(jedisCluster.set(keyBytes, valueBytes)).void
      case Some(d) if d < 1.second =>
        logger.ifWarnEnabled {
          logger.warn(
            s"Because Redis (pre 2.6.12) does not support sub-second expiry, TTL of $d will be rounded up to 1 second"
          )
        } *> F.delay(jedisCluster.setex(keyBytes, 1L, valueBytes)).void
      case Some(d) =>
        F.delay(jedisCluster.setex(keyBytes, d.toSeconds, valueBytes)).void
    }
  }

  override protected def doRemove(key: K): F[Unit] = F.delay {
    jedisCluster.del(keyEncoder.encode(key))
  }.void

  @deprecated(
    "JedisCluster doesn't support this operation, scheduled to be removed with the next jedis major release",
    "0.28.0"
  )
  override protected def doRemoveAll: F[Unit] = F.raiseError {
    new JedisClusterException("No way to dispatch this command to Redis Cluster.")
  }

  override val close: F[Unit] = F.delay(jedisCluster.close())
}
