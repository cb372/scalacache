package scalacache.redis

import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis._
import scalacache._

import scala.collection.JavaConverters._
import scala.language.higherKinds
import scala.util.{Failure, Success, Try}

class SentinelRedisCacheSpec extends RedisCacheSpecBase {

  override type JClient = Jedis
  override type JPool = JedisSentinelPool

  override def withJedis = assumingRedisSentinelIsRunning

  override def constructCache[F[_]: Async](pool: JPool): CacheAlg[F] = SentinelRedisCache[F](pool)

  override def flushRedis(client: JClient): Unit = client.flushDB()

  /**
    * This assumes that Redis master with name "master" and password "master-local" is running,
    * and a sentinel is also running with to monitor this master on port 26379.
    */
  def assumingRedisSentinelIsRunning(f: (JedisSentinelPool, Jedis) => Unit): Unit = {
    Try {
      val jedisPool = new JedisSentinelPool("master", Set("127.0.0.1:26379").asJava, new GenericObjectPoolConfig)
      val jedis = jedisPool.getResource()
      jedis.ping()
      (jedisPool, jedis)
    } match {
      case Failure(_) =>
        alert("Skipping tests because Redis master and sentinel does not appear to be running on localhost.")
      case Success((pool, client)) => f(pool, client)
    }
  }

  runTestsIfPossible()

}
