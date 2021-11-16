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

import cats.effect.IO
import scalacache.serialization.binary.BinaryCodec
import scalacache.serialization.binary.StringBinaryCodec

class RedisCacheSpec extends RedisCacheSpecBase with RedisTestUtil {

  type JClient = JedisClient
  type JPool   = JedisPool

  val withJedis = assumingRedisIsRunning _

  def constructCache[V](pool: JPool)(implicit codec: BinaryCodec[V]): Cache[IO, String, V] =
    new RedisCache[IO, String, V](jedisPool = pool)

  def flushRedis(client: JClient): Unit = client.underlying.flushDB(): Unit

  runTestsIfPossible()

}
