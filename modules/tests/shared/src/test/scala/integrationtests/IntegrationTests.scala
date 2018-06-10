package integrationtests

import java.util.UUID

import org.scalatest._
import cats.effect.{IO => CatsIO}
import monix.eval.{Task => MonixTask}
import scalaz.concurrent.{Task => ScalazTask}
import net.spy.memcached.{AddrUtil, MemcachedClient}
import redis.clients.jedis.JedisPool

import scala.concurrent.duration.Duration
import scala.language.higherKinds
import scala.util.control.NonFatal
import scalacache._
import scalacache.caffeine.CaffeineCache
import scalacache.memcached.MemcachedCache
import scalacache.redis.RedisCache

class IntegrationTests extends WordSpec with Matchers with BeforeAndAfterAll {

  import scalacache.serialization.binary._

  private val memcachedClient = new MemcachedClient(AddrUtil.getAddresses("localhost:11211"))
  private val jedisPool = new JedisPool("localhost", 6379)

  override def afterAll(): Unit = {
    memcachedClient.shutdown()
    jedisPool.close()
  }

  private def memcachedIsRunning: Boolean =
    try {
      memcachedClient.get("foo")
      true
    } catch { case _: Exception => false }

  private def redisIsRunning: Boolean =
    try {
      val jedis = jedisPool.getResource()
      try {
        jedis.ping()
        true
      } finally {
        jedis.close()
      }
    } catch {
      case NonFatal(_) => false
    }

  case class CacheBackend[F[_]: Async](name: String, cache: Cache[F])

  private def caffeine[F[_]: Async] = CacheBackend("Caffeine", CaffeineCache[F])
  private def memcached[F[_]: Async]: Seq[CacheBackend[F]] =
    if (memcachedIsRunning) {
      Seq(
        {
          CacheBackend("(Memcached) ⇔ (binary codec)", MemcachedCache[F](memcachedClient))
        }, {
          CacheBackend("(Memcached) ⇔ (circe codec)", MemcachedCache[F](memcachedClient))
        }
      )
    } else {
      alert("Skipping Memcached integration tests because Memcached does not appear to be running on localhost.")
      Nil
    }

  private def redis[F[_]: Async]: Seq[CacheBackend[F]] =
    if (redisIsRunning)
      Seq(
        {
          CacheBackend("(Redis) ⇔ (binary codec)", RedisCache[F](jedisPool))
        }, {
          CacheBackend("(Redis) ⇔ (circe codec)", RedisCache[F](jedisPool))
        }
      )
    else {
      alert("Skipping Redis integration tests because Redis does not appear to be running on localhost.")
      Nil
    }

  def backends[AIO[_]: Async]: List[CacheBackend[AIO]] = List(caffeine) ++ memcached ++ redis

  def passTests[F[_]](fName: String)(runSync: F[Option[String]] => Option[String])(implicit F: Async[F]): Unit = {
    implicit final class RichF[A](fa: F[A]) {
      def map[B](f: A => B): F[B] = F.map(fa)(f)
      def flatMap[B](f: A => F[B]): F[B] = F.flatMap(fa)(f)
    }

    backends[F].foreach { cacheBackend =>
      s"${cacheBackend.name} ⇔ ($fName) should defer the computation and give the correct result" in {
        val key: String = UUID.randomUUID().toString
        val initialValue: String = UUID.randomUUID().toString
        val cache = cacheBackend.cache

        import scalacache.serialization.binary._

        val program: F[Option[String]] =
          for {
            _ <- cache.put(key)(initialValue)
            readFromCache <- cache.get[String](key)
            updatedValue = s"prepended ${readFromCache.getOrElse("couldn't find in cache!")}"
            _ <- cache.put(key)(updatedValue)
            finalValueFromCache <- cache.get[String](key)
          } yield finalValueFromCache

        checkComputationHasNotRun(key)(runSync)(cache)

        val result: Option[String] = runSync(program)
        assert(result.contains(s"prepended $initialValue"))
      }
    }
  }

  "with cats-effect IO" should {
    import CatsEffect.implicits._
    passTests[CatsIO]("cats-effect IO")(_.unsafeRunSync())
  }

  "with Monix Task" should {
    import Monix.implicits._
    import monix.execution.Scheduler.Implicits.global
    passTests[MonixTask]("Monix Task")(_.runSyncUnsafe(Duration.Inf))
  }

  "with Scalaz Task" should {
    import Scalaz72.implicits._
    passTests[ScalazTask]("Scalaz Task")(_.unsafePerformSync)
  }

  private def checkComputationHasNotRun[F[_]: Async](key: String)(runSync: F[Option[String]] => Option[String])(
      cache: Cache[F]): Unit = {
    Thread.sleep(1000)
    assert(runSync(cache.get[String](key)).isEmpty)
  }

}
