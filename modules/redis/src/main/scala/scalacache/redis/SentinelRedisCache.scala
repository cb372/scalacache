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

import cats.effect.Sync
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis._
import scalacache.serialization.binary.BinaryCodec
import scalacache.serialization.binary.BinaryEncoder

import scala.jdk.CollectionConverters._
import cats.syntax.functor._

/** Thin wrapper around Jedis that works with Redis Sentinel.
  */
class SentinelRedisCache[F[_]: Sync, K, V](val jedisPool: JedisSentinelPool)(implicit
    val keyEncoder: BinaryEncoder[K],
    val codec: BinaryCodec[V]
) extends RedisCacheBase[F, K, V] {

  protected def F: Sync[F] = Sync[F]

  type JClient = Jedis

  protected def doRemoveAll: F[Unit] =
    withJedis { jedis =>
      F.delay(jedis.flushDB()).void
    }

}

object SentinelRedisCache {

  /** Create a `SentinelRedisCache` that uses a `JedisSentinelPool` with a default pool config.
    *
    * @param clusterName
    *   Name of the redis cluster
    * @param sentinels
    *   set of sentinels in format [host1:port, host2:port]
    * @param password
    *   password of the cluster
    */
  def apply[F[_]: Sync, K, V](clusterName: String, sentinels: Set[String], password: String)(implicit
      keyEncoder: BinaryEncoder[K],
      codec: BinaryCodec[V]
  ): SentinelRedisCache[F, K, V] =
    apply(new JedisSentinelPool(clusterName, sentinels.asJava, new GenericObjectPoolConfig[Jedis], password))

  /** Create a `SentinelRedisCache` that uses a `JedisSentinelPool` with a custom pool config.
    *
    * @param clusterName
    *   Name of the redis cluster
    * @param sentinels
    *   set of sentinels in format [host1:port, host2:port]
    * @param password
    *   password of the cluster
    * @param poolConfig
    *   config of the underlying pool
    */
  def apply[F[_]: Sync, K, V](
      clusterName: String,
      sentinels: Set[String],
      poolConfig: GenericObjectPoolConfig[Jedis],
      password: String
  )(implicit
      keyEncoder: BinaryEncoder[K],
      codec: BinaryCodec[V]
  ): SentinelRedisCache[F, K, V] =
    apply(new JedisSentinelPool(clusterName, sentinels.asJava, poolConfig, password))

  /** Create a `SentinelRedisCache` that uses the given JedisSentinelPool
    *
    * @param jedisSentinelPool
    *   a JedisSentinelPool
    */
  def apply[F[_]: Sync, K, V](
      jedisSentinelPool: JedisSentinelPool
  )(implicit keyEncoder: BinaryEncoder[K], codec: BinaryCodec[V]): SentinelRedisCache[F, K, V] =
    new SentinelRedisCache[F, K, V](jedisSentinelPool)

}
