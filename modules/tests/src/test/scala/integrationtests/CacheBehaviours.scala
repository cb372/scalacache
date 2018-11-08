package integrationtests
import java.util.UUID

import cats.effect.{IO => CatsIO}
import com.twitter.util.{Await => TwitterAwait, Future => TwitterFuture}
import monix.eval.{Task => MonixTask}
import org.scalatest.FlatSpec
import scalacache.{Cache, _}
import scalaz.concurrent.{Task => ScalazTask}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.language.higherKinds

trait CacheBehaviours {
  this: FlatSpec =>

  def cacheWithDifferentEffects[T](name: String, cache: => Cache[String]) {
    s"$name ⇔ (cats-effect IO)" should "defer the computation and give the correct result" in {
      implicit val theCache: Cache[String] = cache
      implicit val mode: Mode[CatsIO] = CatsEffect.modes.io

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

    s"$name ⇔ (Twitter Future)" should "defer the computation and give the correct result" in {
      implicit val theCache: Cache[String] = cache
      implicit val mode: Mode[TwitterFuture] = TwitterUtil.modes.future

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

      val result: Option[String] = TwitterAwait.result(program)
      assert(result.contains("prepended " + initialValue))
    }
  }

  private def checkComputationHasNotRun(key: String)(implicit cache: Cache[String]): Unit = {
    Thread.sleep(1000)
    implicit val mode: Mode[Id] = scalacache.modes.sync.mode
    assert(scalacache.sync.get(key).isEmpty)
  }
}
