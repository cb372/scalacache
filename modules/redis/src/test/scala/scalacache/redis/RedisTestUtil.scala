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

import org.scalatest.Alerting
import redis.clients.jedis._

import scala.util.{Success, Failure, Try}

trait RedisTestUtil { self: Alerting =>

  def assumingRedisIsRunning(f: (JedisPool, JedisClient) => Unit): Unit = {
    Try {
      val jedisPool = new JedisPool("localhost", 6379)
      val jedis     = jedisPool.getResource()
      jedis.ping()
      (jedisPool, new JedisClient(jedis))
    } match {
      case Failure(_) =>
        alert("Skipping tests because Redis does not appear to be running on localhost.")
      case Success((pool, client)) => f(pool, client)
    }
  }

}
