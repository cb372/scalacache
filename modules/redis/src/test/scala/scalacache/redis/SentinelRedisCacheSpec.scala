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

import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis._

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}
import scalacache._
import cats.effect.IO
import scalacache.serialization.binary.BinaryCodec
import scalacache.serialization.binary.StringBinaryCodec

class SentinelRedisCacheSpec extends RedisCacheSpecBase {

  type JClient = JedisClient
  type JPool   = JedisSentinelPool

  val withJedis = assumingRedisSentinelIsRunning _

  def constructCache[V](pool: JPool)(implicit codec: BinaryCodec[V]): Cache[IO, String, V] =
    new SentinelRedisCache[IO, String, V](jedisPool = pool)

  def flushRedis(client: JClient): Unit = client.underlying.flushDB(): Unit

  /** This assumes that Redis master with name "master" and password "master-local" is running, and a sentinel is also
    * running with to monitor this master on port 26379.
    */
  def assumingRedisSentinelIsRunning(f: (JedisSentinelPool, JedisClient) => Unit): Unit = {
    Try {
      val jedisPool = new JedisSentinelPool("master", Set("127.0.0.1:26379").asJava, new GenericObjectPoolConfig[Jedis])
      val jedis     = jedisPool.getResource()
      jedis.ping()
      (jedisPool, new JedisClient(jedis))
    } match {
      case Failure(_) =>
        alert("Skipping tests because Redis master and sentinel does not appear to be running on localhost.")
      case Success((pool, client)) => f(pool, client)
    }
  }

  runTestsIfPossible()

}
