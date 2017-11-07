package scalacache.redis

import redis.clients.jedis._

import scala.language.postfixOps
import scalacache._
import scalacache.serialization.Codec

class RedisCacheSpec extends RedisCacheSpecBase with RedisTestUtil {

  type JClient = Jedis
  type JPool = JedisPool

  val withJedis = assumingRedisIsRunning _

  def constructCache[V](pool: JPool)(implicit codec: Codec[V]): CacheAlg[V] =
    new RedisCache[V](jedisPool = pool)

  def flushRedis(client: JClient): Unit = client.flushDB()

  runTestsIfPossible()

}
