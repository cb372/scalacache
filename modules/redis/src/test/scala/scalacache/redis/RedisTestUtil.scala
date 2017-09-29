package scalacache.redis

import org.scalatest.Alerting
import redis.clients.jedis._

import scala.util.{Success, Failure, Try}

trait RedisTestUtil { self: Alerting =>

  def assumingRedisIsRunning(f: (JedisPool, Jedis) => Unit): Unit = {
    Try {
      val jedisPool = new JedisPool("localhost", 6379)
      val jedis = jedisPool.getResource()
      jedis.ping()
      (jedisPool, jedis)
    } match {
      case Failure(_) =>
        alert("Skipping tests because Redis does not appear to be running on localhost.")
      case Success((pool, client)) => f(pool, client)
    }
  }

}
