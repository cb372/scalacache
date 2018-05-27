package scalacache.redis

import redis.clients.jedis.{JedisPoolConfig, JedisShardInfo, ShardedJedis, ShardedJedisPool}

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._
import scalacache._
import scala.language.higherKinds

class ShardedRedisCacheSpec extends RedisCacheSpecBase {

  override type JClient = ShardedJedis
  override type JPool = ShardedJedisPool

  override val withJedis = assumingMultipleRedisAreRunning

  override def constructCache[F[_]: Mode](pool: JPool): CacheAlg[F] = ShardedRedisCache[F](pool)

  override def flushRedis(client: JClient): Unit =
    client.getAllShards.asScala.foreach(_.flushDB())

  def assumingMultipleRedisAreRunning(f: (ShardedJedisPool, ShardedJedis) => Unit): Unit = {
    Try {
      val shard1 = new JedisShardInfo("localhost", 6379)
      val shard2 = new JedisShardInfo("localhost", 6380)

      val jedisPool =
        new ShardedJedisPool(new JedisPoolConfig(), java.util.Arrays.asList(shard1, shard2))
      val jedis = jedisPool.getResource

      jedis.getAllShards.asScala.foreach(_.ping())

      (jedisPool, jedis)
    } match {
      case Failure(_) =>
        alert("Skipping tests because it does not appear that multiple instances of Redis are running on localhost.")
      case Success((pool, client)) => f(pool, client)
    }
  }

  runTestsIfPossible()

}
