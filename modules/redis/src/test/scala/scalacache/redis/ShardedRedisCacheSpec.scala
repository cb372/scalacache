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

import redis.clients.jedis._
import scalacache._

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}
import cats.effect.IO
import scalacache.serialization.binary.BinaryCodec
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import scalacache.serialization.binary.StringBinaryCodec

class ShardedRedisCacheSpec extends RedisCacheSpecBase {

  type JClient = ShardedJedisClient
  type JPool   = ShardedJedisPool

  val withJedis = assumingMultipleRedisAreRunning _

  def constructCache[V](pool: JPool)(implicit codec: BinaryCodec[V]): Cache[IO, String, V] =
    new ShardedRedisCache[IO, String, V](jedisPool = pool)

  def flushRedis(client: JClient): Unit =
    client.underlying.getAllShards.asScala.foreach(_.flushDB())

  def assumingMultipleRedisAreRunning(f: (ShardedJedisPool, ShardedJedisClient) => Unit): Unit = {
    Try {
      val shard1 = new JedisShardInfo("localhost", 6379)
      val shard2 = new JedisShardInfo("localhost", 6380)

      val jedisPool =
        new ShardedJedisPool(new GenericObjectPoolConfig[ShardedJedis], java.util.Arrays.asList(shard1, shard2))
      val jedis = jedisPool.getResource

      jedis.getAllShards.asScala.foreach(_.ping())

      (jedisPool, new ShardedJedisClient(jedis))
    } match {
      case Failure(_) =>
        alert("Skipping tests because it does not appear that multiple instances of Redis are running on localhost.")
      case Success((pool, client)) => f(pool, client)
    }
  }

  runTestsIfPossible()

}
