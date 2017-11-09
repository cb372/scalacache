package integrationtests

import java.util.UUID

import org.scalatest._
import cats.effect.{IO => CatsIO}
import monix.eval.{Task => MonixTask}

import scalaz.concurrent.{Task => ScalazTask}
import net.spy.memcached.{AddrUtil, MemcachedClient}
import redis.clients.jedis.JedisPool

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.language.higherKinds
import scala.util.control.NonFatal
import scalacache._
import scalacache.caffeine.CaffeineCache
import scalacache.memcached.MemcachedCache
import scalacache.redis.RedisCache

class IntegrationTests extends FlatSpec with Matchers with BeforeAndAfterAll {

  private val memcachedClient = new MemcachedClient(AddrUtil.getAddresses("localhost:11211"))
  private val jedisPool = new JedisPool("localhost", 6379)

  override def afterAll(): Unit = {
    memcachedClient.shutdown()
    jedisPool.close()
  }

  private def memcachedIsRunning: Boolean = {
    try {
      memcachedClient.get("foo")
      true
    } catch { case _: Exception => false }
  }

  private def redisIsRunning: Boolean = {
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
  }

  case class CacheBackend(name: String, cache: Cache[String])

  private val caffeine = CacheBackend("Caffeine", CaffeineCache[String])
  private val memcached: Seq[CacheBackend] =
    if (memcachedIsRunning) {
      Seq(
        {
          import scalacache.serialization.binary._
          CacheBackend("(Memcached) ⇔ (binary codec)", MemcachedCache[String](memcachedClient))
        }, {
          import scalacache.serialization.circe._
          CacheBackend("(Memcached) ⇔ (circe codec)", MemcachedCache[String](memcachedClient))
        }
      )
    } else {
      alert("Skipping Memcached integration tests because Memcached does not appear to be running on localhost.")
      Nil
    }

  private val redis: Seq[CacheBackend] =
    if (redisIsRunning)
      Seq(
        {
          import scalacache.serialization.binary._
          CacheBackend("(Redis) ⇔ (binary codec)", RedisCache[String](jedisPool))
        }, {
          import scalacache.serialization.circe._
          CacheBackend("(Redis) ⇔ (circe codec)", RedisCache[String](jedisPool))
        }
      )
    else {
      alert("Skipping Redis integration tests because Redis does not appear to be running on localhost.")
      Nil
    }

  val backends: List[CacheBackend] = List(caffeine) ++ memcached ++ redis

  for (CacheBackend(name, cache) <- backends) {

    s"$name ⇔ (cats-effect IO)" should "defer the computation and give the correct result" in {
      implicit val theCache: Cache[String] = cache
      implicit val mode: Mode[CatsIO] = CatsEffect.modes.io

      val key = UUID.randomUUID().toString
      val initialValue = UUID.randomUUID().toString

      import cats.syntax.all._
      val program =
        for {
          _ <- put(key)(initialValue)
          readFromCache <- get(key)
          updatedValue = "prepended " + readFromCache.getOrElse("couldn't find in cache!")
          _ <- put(key)(updatedValue)
          finalValueFromCache <- get(key)
        } yield finalValueFromCache

      checkComputationHasNotRun(key)

      val result: Option[String] = program.unsafeRunSync()
      assert(result.contains("prepended " + initialValue))
    }

    s"$name ⇔ (Monix Task)" should "defer the computation and give the correct result" in {
      implicit val theCache: Cache[String] = cache
      implicit val mode: Mode[MonixTask] = Monix.modes.task

      val key = UUID.randomUUID().toString
      val initialValue = UUID.randomUUID().toString

      val program =
        for {
          _ <- put(key)(initialValue)
          readFromCache <- get(key)
          updatedValue = "prepended " + readFromCache.getOrElse("couldn't find in cache!")
          _ <- put(key)(updatedValue)
          finalValueFromCache <- get(key)
        } yield finalValueFromCache

      checkComputationHasNotRun(key)

      val future = program.runAsync(monix.execution.Scheduler.global)
      val result = Await.result(future, Duration.Inf)
      assert(result.contains("prepended " + initialValue))
    }

    s"$name ⇔ (Scalaz Task)" should "defer the computation and give the correct result" in {
      implicit val theCache: Cache[String] = cache
      implicit val mode: Mode[ScalazTask] = Scalaz72.modes.task

      val key = UUID.randomUUID().toString
      val initialValue = UUID.randomUUID().toString

      val program =
        for {
          _ <- put(key)(initialValue)
          readFromCache <- get(key)
          updatedValue = "prepended " + readFromCache.getOrElse("couldn't find in cache!")
          _ <- put(key)(updatedValue)
          finalValueFromCache <- get(key)
        } yield finalValueFromCache

      checkComputationHasNotRun(key)

      val result: Option[String] = program.unsafePerformSync
      assert(result.contains("prepended " + initialValue))
    }

  }

  private def checkComputationHasNotRun(key: String)(implicit cache: Cache[String]): Unit = {
    Thread.sleep(1000)
    implicit val mode: Mode[Id] = scalacache.modes.sync.mode
    assert(scalacache.sync.get(key).isEmpty)
  }

}
