package scalacache.redis

import redis.clients.jedis._
import scalacache._
import scalacache.serialization.Codec

import scala.collection.JavaConverters._
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
