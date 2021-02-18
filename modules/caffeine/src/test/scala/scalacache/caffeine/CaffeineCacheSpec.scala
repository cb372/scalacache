package scalacache.caffeine

import java.time.Instant

import cats.effect.testkit.TestContext
import cats.effect.unsafe.{IORuntime, Scheduler}
import cats.effect.{Clock, IO, Sync, SyncIO}
import com.github.benmanes.caffeine.cache.Caffeine
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfter
import scalacache._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CaffeineCacheSpec extends AnyFlatSpec with Matchers with BeforeAndAfter with ScalaFutures {

  private val deterministicRuntime: (TestContext, IORuntime) = {

    val ctx = TestContext()
    val scheduler = new Scheduler {
      def sleep(delay: FiniteDuration, action: Runnable): Runnable = {
        val cancel = ctx.schedule(delay, action)
        () => cancel()
      }

      def nowMillis()      = ctx.now().toMillis
      def monotonicNanos() = ctx.now().toNanos
    }

    val runtime = IORuntime(ctx, ctx, scheduler, () => ())

    (ctx, runtime)
  }

  private implicit val runTime: IORuntime   = deterministicRuntime._2
  private val ctx: TestContext              = deterministicRuntime._1
  private implicit val ec: ExecutionContext = ctx.derive()

  private def newCCache = Caffeine.newBuilder.build[String, Entry[String]]

  private def newFCache[F[_]: Sync, V](
      underlying: com.github.benmanes.caffeine.cache.Cache[String, Entry[V]]
  ) = {
    CaffeineCache[F, V](underlying)
  }

  private def newIOCache[V](
      underlying: com.github.benmanes.caffeine.cache.Cache[String, Entry[V]]
  ) = {
    newFCache[SyncIO, V](underlying)
  }

  behavior of "get"

  it should "return the value stored in the underlying cache" in {
    val underlying = newCCache
    val entry      = Entry("hello", expiresAt = None)
    underlying.put("key1", entry)
    val result = newIOCache(underlying).get("key1").unsafeRunSync()
    result should be(Some("hello"))
  }

  it should "return None if the given key does not exist in the underlying cache" in {
    val underlying = newCCache
    newIOCache(underlying).get("non-existent key").unsafeRunSync() should be(None)
  }

  it should "return None if the given key exists but the value has expired" in {
    val underlying = newCCache
    val expiredEntry =
      Entry("hello", expiresAt = Some(Instant.now.minusSeconds(1)))
    underlying.put("key1", expiredEntry)
    newIOCache(underlying).get("key1").unsafeRunSync() should be(None)
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache with no TTL" in {
    val underlying = newCCache
    newIOCache(underlying).put("key1")("hello", None).unsafeRunSync()

    underlying.getIfPresent("key1") should be(Entry("hello", None))
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache with the given TTL" in {
    val now = Instant.ofEpochMilli(ctx.now.toMillis)

    val underlying = newCCache
    newFCache[IO, String](underlying).put("key1")("hello", Some(10.seconds)).unsafeToFuture().map { _ =>
      underlying.getIfPresent("key1") should be(Entry("hello", expiresAt = Some(now.plusSeconds(10))))

    }
    ctx.tick()
  }

  it should "support a TTL greater than Int.MaxValue millis" in {
    val now = Instant.ofEpochMilli(ctx.now.toMillis)

    val underlying = newCCache
    newFCache[IO, String](underlying).put("key1")("hello", Some(30.days)).unsafeToFuture().map { _ =>
      underlying.getIfPresent("key1") should be(
        Entry("hello", expiresAt = Some(now.plusMillis(30.days.toMillis)))
      )
    }
    ctx.tick()
  }

  behavior of "remove"

  it should "delete the given key and its value from the underlying cache" in {
    val underlying = newCCache
    val entry      = Entry("hello", expiresAt = None)
    underlying.put("key1", entry)
    underlying.getIfPresent("key1") should be(entry)

    newIOCache(underlying).remove("key1").unsafeRunSync()
    underlying.getIfPresent("key1") should be(null)
  }

}
