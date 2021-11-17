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
import scalacache.serialization.binary.StringBinaryCodec
import scala.annotation.nowarn

class RedisClusterCacheSpec extends RedisCacheSpecBase with RedisTestUtil {

  type JClient = JedisClusterClient
  type JPool   = JedisCluster

  override val withJedis = assumingRedisClusterIsRunning _

  def constructCache[V](jedisCluster: JedisCluster)(implicit codec: BinaryCodec[V]): Cache[IO, String, V] =
    new RedisClusterCache[IO, String, V](jedisCluster)

  def flushRedis(client: JClient): Unit = {
    val _ = client.underlying.getClusterNodes.asScala.mapValues(_.getResource.flushDB()): @nowarn
  }

  def assumingRedisClusterIsRunning(f: (JPool, JClient) => Unit): Unit = {
    Try {
      val jedisCluster = new JedisCluster(
        Set(
          new HostAndPort("localhost", 7000),
          new HostAndPort("localhost", 7001),
          new HostAndPort("localhost", 7002),
          new HostAndPort("localhost", 7003),
          new HostAndPort("localhost", 7004),
          new HostAndPort("localhost", 7005)
        ).asJava
      )

      if (jedisCluster.getClusterNodes.asScala.isEmpty)
        throw new IllegalStateException("No connections initialized")
      else jedisCluster.getClusterNodes.asScala.mapValues(_.getResource.ping()): @nowarn

      (jedisCluster, new JedisClusterClient(jedisCluster))
    } match {
      case Failure(_) => alert("Skipping tests because it does not appear Redis Cluster is running on localhost.")
      case Success((pool, client)) => f(pool, client)
    }
  }

  runTestsIfPossible()

}
