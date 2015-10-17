package scalacache.redis

import org.scalatest.Alerting
import redis.clients.jedis._
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.util.{ Success, Failure, Try }

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

  def assumingMultipleRedisAreRunning(f: (ShardedJedisPool, ShardedJedis) => Unit): Unit = {
    import scala.collection.JavaConversions.seqAsJavaList

    Try {
      val shard1 = new JedisShardInfo("localhost", 6379)
      val shard2 = new JedisShardInfo("localhost", 6380)

      val jedisPool = new ShardedJedisPool(new JedisPoolConfig(), Seq(shard1, shard2))
      val jedis = jedisPool.getResource

      jedis.getAllShards.foreach(_.ping())

      (jedisPool, jedis)
    } match {
      case Failure(_) => alert("Skipping tests because Redis does not appear to be running on localhost.")
      case Success((pool, client)) =>
        f(pool, client)
    }
  }

}
