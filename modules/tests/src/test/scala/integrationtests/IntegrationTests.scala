package integrationtests

import java.util.UUID

import org.scalatest._
import cats.effect.{IO => CatsIO}
import monix.eval.{Task => MonixTask}
import monix.execution.Scheduler

import scalaz.concurrent.{Task => ScalazTask}
import net.spy.memcached.{AddrUtil, MemcachedClient}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.language.higherKinds
import scalacache._
import scalacache.caffeine.CaffeineCache
import scalacache.memcached.MemcachedCache

class IntegrationTests extends FlatSpec with Matchers with BeforeAndAfterAll {

  private val memcachedClient = new MemcachedClient(AddrUtil.getAddresses("localhost:11211"))

  override def afterAll(): Unit = {
    memcachedClient.shutdown()
  }

  def memcachedIsRunning: Boolean = {
    try {
      memcachedClient.get("foo")
      true
    } catch { case _: Exception => false }
  }

  case class CacheBackend(name: String, cache: Cache[String])

  private val caffeine = CacheBackend("caffeine", CaffeineCache[String])
  private val memcached: Option[CacheBackend] =
    if (memcachedIsRunning)
      Some(CacheBackend("memcached", MemcachedCache[String](memcachedClient)))
    else
      None

  val backends: List[CacheBackend] = List(Some(caffeine), memcached).flatten

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
