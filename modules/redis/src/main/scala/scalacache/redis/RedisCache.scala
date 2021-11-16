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
import cats.syntax.functor._
import redis.clients.jedis._
import scalacache.serialization.binary.BinaryCodec
import scalacache.serialization.binary.BinaryEncoder

/** Thin wrapper around Jedis
  */
class RedisCache[F[_]: Sync, K, V](val jedisPool: JedisPool)(implicit
    val keyEncoder: BinaryEncoder[K],
    val codec: BinaryCodec[V]
) extends RedisCacheBase[F, K, V] {

  protected def F: Sync[F] = Sync[F]
  type JClient = Jedis

  protected val doRemoveAll: F[Unit] = withJedis { jedis =>
    F.delay(jedis.flushDB()).void
  }
}

object RedisCache {

  /** Create a Redis client connecting to the given host and use it for caching
    */
  def apply[F[_]: Sync, K, V](
      host: String,
      port: Int
  )(implicit keyEncoder: BinaryEncoder[K], codec: BinaryCodec[V]): RedisCache[F, K, V] =
    apply(new JedisPool(host, port))

  /** Create a cache that uses the given Jedis client pool
    * @param jedisPool
    *   a Jedis pool
    */
  def apply[F[_]: Sync, K, V](
      jedisPool: JedisPool
  )(implicit keyEncoder: BinaryEncoder[K], codec: BinaryCodec[V]): RedisCache[F, K, V] =
    new RedisCache[F, K, V](jedisPool)

}
