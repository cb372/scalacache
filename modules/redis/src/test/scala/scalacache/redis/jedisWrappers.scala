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

/** Scala wrapper for Jedis implementations. This allows an implementation of [[RedisCacheSpecBase]] to choose the
  * specific client required for running the tests.
  */
trait BaseJedisClient {

  def set(key: Array[Byte], value: Array[Byte]): Unit

  def get(key: Array[Byte]): Array[Byte]

  def get(key: String): String

  def ttl(key: Array[Byte]): Long

  def pttl(key: String): Long

}

class JedisClusterClient(val underlying: JedisCluster) extends BaseJedisClient {

  override def set(key: Array[Byte], value: Array[Byte]): Unit =
    underlying.set(key, value): Unit

  override def get(key: Array[Byte]): Array[Byte] =
    underlying.get(key)

  override def get(key: String): String =
    underlying.get(key)

  override def ttl(key: Array[Byte]): Long =
    underlying.ttl(key)

  override def pttl(key: String): Long =
    underlying.pttl(key)
}

class JedisClient(val underlying: Jedis) extends BaseJedisClient {

  override def set(key: Array[Byte], value: Array[Byte]): Unit =
    underlying.set(key, value): Unit

  override def get(key: Array[Byte]): Array[Byte] =
    underlying.get(key)

  override def get(key: String): String =
    underlying.get(key)

  override def ttl(key: Array[Byte]): Long =
    underlying.ttl(key)

  override def pttl(key: String): Long =
    underlying.pttl(key)
}

class ShardedJedisClient(val underlying: ShardedJedis) extends BaseJedisClient {

  override def set(key: Array[Byte], value: Array[Byte]): Unit =
    underlying.set(key, value): Unit

  override def get(key: Array[Byte]): Array[Byte] =
    underlying.get(key)

  override def get(key: String): String =
    underlying.get(key)

  override def ttl(key: Array[Byte]): Long =
    underlying.ttl(key)

  override def pttl(key: String): Long =
    underlying.pttl(key)
}
