package scalacache.caffeine

import java.time.Instant

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import cats.effect.Clock
import cats.effect.IO
import cats.effect.Sync
import cats.effect.SyncIO
import cats.effect.testkit.TestContext
import cats.effect.testkit.TestInstances
import cats.effect.unsafe.IORuntime
import cats.effect.unsafe.Scheduler
import com.github.benmanes.caffeine.cache.Caffeine
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalacache._
import org.scalatest.compatible.Assertion
import cats.effect.kernel.Outcome

class CaffeineCacheSpec extends AnyFlatSpec with Matchers with BeforeAndAfter with ScalaFutures with TestInstances {

  private def ticked[A](f: Ticker => IO[Assertion]): Assertion = {
    implicit val ticker = Ticker(TestContext())

    unsafeRun(f(ticker)) shouldBe Outcome.succeeded(Some(succeed))
  }

  private def newCCache = Caffeine.newBuilder.build[Int, Entry[String]]

  private def newFCache[F[_]: Sync, V](
      underlying: com.github.benmanes.caffeine.cache.Cache[Int, Entry[V]]
  ) = {
    CaffeineCache[F, Int, V](underlying)
  }

  private def newIOCache[V](
      underlying: com.github.benmanes.caffeine.cache.Cache[Int, Entry[V]]
  ) = {
    newFCache[IO, V](underlying)
  }

  behavior of "get"

  it should "return the value stored in the underlying cache" in ticked { _ =>
    val underlying = newCCache
    val entry      = Entry("hello", expiresAt = None)
    underlying.put(1, entry)

    newIOCache(underlying).get(1).map(_ shouldBe Some("hello"))
  }

  it should "return None if the given key does not exist in the underlying cache" in ticked { _ =>
    val underlying = newCCache
    newIOCache(underlying).get(2).map(_ shouldBe None)
  }

  it should "return None if the given key exists but the value has expired" in ticked { _ =>
    val underlying = newCCache
    val expiredEntry =
      Entry("hello", expiresAt = Some(Instant.now.minusSeconds(1)))
    underlying.put(1, expiredEntry)
    newIOCache(underlying).get(1).map(_ shouldBe None)
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache with no TTL" in ticked { _ =>
    val underlying = newCCache
    newIOCache(underlying).put(1)("hello", None) *>
      IO { underlying.getIfPresent(1) }
        .map(_ shouldBe Entry("hello", None))
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache with the given TTL" in ticked { implicit ticker =>
    val ctx = ticker.ctx
    val now = Instant.ofEpochMilli(ctx.now().toMillis)

    val underlying = newCCache

    newFCache[IO, String](underlying).put(1)("hello", Some(10.seconds)).map { _ =>
      underlying.getIfPresent(1) should be(Entry("hello", expiresAt = Some(now.plusSeconds(10))))
    }
  }

  it should "support a TTL greater than Int.MaxValue millis" in ticked { implicit ticker =>
    val ctx = ticker.ctx
    val now = Instant.ofEpochMilli(ctx.now().toMillis)

    val underlying = newCCache
    newFCache[IO, String](underlying).put(1)("hello", Some(30.days)).map { _ =>
      underlying.getIfPresent(1) should be(
        Entry("hello", expiresAt = Some(now.plusMillis(30.days.toMillis)))
      )
    }
  }

  behavior of "remove"

  it should "delete the given key and its value from the underlying cache" in ticked { _ =>
    val underlying = newCCache
    val entry      = Entry("hello", expiresAt = None)
    underlying.put(1, entry)
    underlying.getIfPresent(1) should be(entry)

    newIOCache(underlying).remove(1) *>
      IO(underlying.getIfPresent(1)).map(_ shouldBe null)
  }

}
