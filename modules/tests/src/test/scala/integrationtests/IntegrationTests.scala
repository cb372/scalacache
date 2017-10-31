package integrationtests

import java.util.UUID

import org.scalatest._
import cats.effect.{IO => CatsIO}
import monix.eval.{Task => MonixTask}
import monix.execution.Scheduler

import scalaz.concurrent.{Task => ScalazTask}
import net.spy.memcached.{AddrUtil, MemcachedClient}
import redis.clients.jedis.JedisPool

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.language.higherKinds
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
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

  case class CacheBackend(name: String, cache: scalacache.Cache[String])

  private val caffeine = CacheBackend("Caffeine", CaffeineCache[String])
  private val memcached: Option[CacheBackend] =
    if (memcachedIsRunning)
      Some(CacheBackend("Memcached", MemcachedCache[String](memcachedClient)))
    else {
      alert("Skipping Memcached integration tests because Memcached does not appear to be running on localhost.")
      None
    }

  private val redis: Option[CacheBackend] =
    if (redisIsRunning)
      Some(CacheBackend("Redis", RedisCache[String](jedisPool)))
    else {
      alert("Skipping Redis integration tests because Redis does not appear to be running on localhost.")
      None
    }

  val backends: List[CacheBackend] = List(Some(caffeine), memcached, redis).flatten

  import scalacache._

  for (CacheBackend(name, cache) <- backends) {

    s"($name) ⇔ (cats-effect IO)" should "defer the computation and give the correct result" in {
      implicit val theCache: Cache[String] = cache
      implicit val mode: Mode[CatsIO] = scalacache.cats.effect.modes.io

      val key = UUID.randomUUID().toString
      val initialValue = UUID.randomUUID().toString

      import _root_.cats.syntax.all._
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

    s"($name) ⇔ (Monix Task)" should "defer the computation and give the correct result" in {
      implicit val theCache: Cache[String] = cache
      implicit val mode: Mode[MonixTask] = scalacache.monix.modes.task

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

      val scheduler: Scheduler = _root_.monix.execution.Scheduler.global
      val future = program.runAsync(scheduler)
      val result = Await.result(future, Duration.Inf)
      assert(result.contains("prepended " + initialValue))
    }

    s"($name) ⇔ (Scalaz Task)" should "defer the computation and give the correct result" in {
      implicit val theCache: Cache[String] = cache
      implicit val mode: Mode[ScalazTask] = scalacache.scalaz72.modes.task

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
