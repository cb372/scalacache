package scalacache.redis

import redis.clients.jedis._
import scalacache._
import scalacache.serialization.Codec

import scala.language.postfixOps
import cats.effect.IO
import scalacache.serialization.binary.BinaryCodec
import scalacache.serialization.binary.StringBinaryCodec

class RedisCacheSpec extends RedisCacheSpecBase with RedisTestUtil {

  type JClient = JedisClient
  type JPool   = JedisPool

  val withJedis = assumingRedisIsRunning _

  def constructCache[V](pool: JPool)(implicit codec: BinaryCodec[V]): Cache[IO, String, V] =
    new RedisCache[IO, String, V](jedisPool = pool)

  def flushRedis(client: JClient): Unit = client.underlying.flushDB()

  runTestsIfPossible()

}
