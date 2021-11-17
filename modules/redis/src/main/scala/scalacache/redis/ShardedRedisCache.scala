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
import redis.clients.jedis._
import scalacache.serialization.binary.{BinaryCodec, BinaryEncoder}
import org.apache.commons.pool2.impl.GenericObjectPoolConfig

import scala.jdk.CollectionConverters._

/** Thin wrapper around Jedis that works with sharded Redis.
  */
class ShardedRedisCache[F[_]: Sync, K, V](val jedisPool: ShardedJedisPool)(implicit
    val keyEncoder: BinaryEncoder[K],
    val codec: BinaryCodec[V]
) extends RedisCacheBase[F, K, V] {

  protected def F: Sync[F] = Sync[F]

  type JClient = ShardedJedis

  protected val doRemoveAll: F[Unit] = withJedis { jedis =>
    F.delay {
      jedis.getAllShards.asScala.foreach(_.flushDB())
    }
  }

}

object ShardedRedisCache {

  /** Create a sharded Redis client connecting to the given hosts and use it for caching
    */
  def apply[F[_]: Sync, K, V](
      hosts: (String, Int)*
  )(implicit keyEncoder: BinaryEncoder[K], codec: BinaryCodec[V]): ShardedRedisCache[F, K, V] = {
    val shards = hosts.map { case (host, port) =>
      new JedisShardInfo(host, port)
    }
    val pool = new ShardedJedisPool(new GenericObjectPoolConfig[ShardedJedis], shards.asJava)
    apply(pool)
  }

  /** Create a cache that uses the given ShardedJedis client pool
    * @param jedisPool
    *   a ShardedJedis pool
    */
  def apply[F[_]: Sync, K, V](
      jedisPool: ShardedJedisPool
  )(implicit keyEncoder: BinaryEncoder[K], codec: BinaryCodec[V]): ShardedRedisCache[F, K, V] =
    new ShardedRedisCache[F, K, V](jedisPool)

}
