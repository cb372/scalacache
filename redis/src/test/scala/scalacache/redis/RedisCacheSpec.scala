package scalacache.redis

import redis.clients.jedis._

import scala.language.postfixOps
import scalacache.Cache

class RedisCacheSpec
    extends RedisCacheSpecBase
    with RedisTestUtil {

  type JClient = Jedis
  type JPool = JedisPool

  val withJedis = assumingRedisIsRunning _

  def constructCache(pool: JPool, useLegacySerialization: Boolean): Cache[Array[Byte]] = new RedisCache(jedisPool = pool, useLegacySerialization = useLegacySerialization)

  def flushRedis(client: JClient): Unit = client.flushDB()

  runTestsIfPossible()

}
