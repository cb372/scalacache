package scalacache.redis

import redis.clients.jedis._
import scalacache._
import scalacache.serialization.Codec

import scala.language.postfixOps

class RedisCacheSpec extends RedisCacheSpecBase with RedisTestUtil {

  type JClient = JedisClient
  type JPool   = JedisPool

  val withJedis = assumingRedisIsRunning _

  def constructCache[V](pool: JPool)(implicit codec: Codec[V]): CacheAlg[V] =
    new RedisCache[V](jedisPool = pool)

  def flushRedis(client: JClient): Unit = client.underlying.flushDB()

  runTestsIfPossible()

}
