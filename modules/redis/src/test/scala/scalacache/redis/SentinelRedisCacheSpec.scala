package scalacache.redis

import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import scalacache._
import scalacache.serialization.Codec
import cats.effect.IO
import scalacache.serialization.binary.BinaryCodec
import scalacache.serialization.binary.StringBinaryCodec

class SentinelRedisCacheSpec extends RedisCacheSpecBase {

  type JClient = JedisClient
  type JPool   = JedisSentinelPool

  val withJedis = assumingRedisSentinelIsRunning _

  def constructCache[V](pool: JPool)(implicit codec: BinaryCodec[V]): Cache[IO, String, V] =
    new SentinelRedisCache[IO, String, V](jedisPool = pool)

  def flushRedis(client: JClient): Unit = client.underlying.flushDB()

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
