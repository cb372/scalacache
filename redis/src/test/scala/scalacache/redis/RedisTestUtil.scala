package scalacache.redis

import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.scalatest.Alerting
import redis.clients.jedis.{ JedisSentinelPool, JedisPool, Jedis }

import scala.util.{ Success, Failure, Try }
import scala.collection.JavaConversions._

trait RedisTestUtil { self: Alerting =>

  def assumingRedisIsRunning(f: (JedisPool, Jedis) => Unit): Unit = {
    Try {
      val jedisPool = new JedisPool("localhost", 6379)
      val jedis = jedisPool.getResource()
      jedis.ping()
      (jedisPool, jedis)
    } match {
      case Failure(_) => alert("Skipping tests because Redis does not appear to be running on localhost.")
      case Success((pool, client)) =>
        f(pool, client)
    }
  }

  /**
   * This assumes that redis master with name "master" and password "master-local" is running.
   * And a sentinel is also running with to monitor this master on port 26379
   *
   * @param f
   */
  def assumingRedisSentinelIsRunning(f: (JedisSentinelPool, Jedis) => Unit): Unit = {
    Try {
      val jedisSentinelPool = new JedisSentinelPool("master", Set("127.0.0.1:26379"), new GenericObjectPoolConfig, "master-local")
      val jedis = jedisSentinelPool.getResource()
      jedis.ping()
      (jedisSentinelPool, jedis)
    } match {
      case Failure(_) => alert("Skipping tests because Redis master and sentinel does not appear to be running on localhost.")
      case Success((pool, client)) =>
        f(pool, client)
    }
  }

}
