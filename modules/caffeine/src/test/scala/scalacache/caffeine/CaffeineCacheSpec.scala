/*
 * Copyright 2021 scalacache
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package scalacache.caffeine

import java.time.Instant

import scala.concurrent.duration._

import cats.effect.Clock
import cats.effect.IO
import cats.effect.Sync
import cats.effect.testkit.TestContext
import cats.effect.testkit.TestInstances
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

  case class MyInt(int: Int)

  private def newCCache = Caffeine.newBuilder.build[MyInt, Entry[String]]

  private def newFCache[F[_]: Sync, V](
      underlying: com.github.benmanes.caffeine.cache.Cache[MyInt, Entry[V]]
  ) = {
    CaffeineCache[F, MyInt, V](underlying)
  }

  private def newIOCache[V](
      underlying: com.github.benmanes.caffeine.cache.Cache[MyInt, Entry[V]]
  ) = {
    newFCache[IO, V](underlying)
  }

  behavior of "get"

  it should "return the value stored in the underlying cache if expiration is not specified" in ticked { _ =>
    val underlying = newCCache
    val entry      = Entry("hello", expiresAt = None)
    underlying.put(MyInt(1), entry)

    newIOCache(underlying).get(MyInt(1)).map(_ shouldBe Some("hello"))
  }

  it should "return None if the given key does not exist in the underlying cache" in ticked { _ =>
    val underlying = newCCache
    newIOCache(underlying).get(MyInt(2)).map(_ shouldBe None)
  }

  it should "return None if the given key exists but the value has expired" in ticked { _ =>
    Clock[IO].monotonic.flatMap { now =>
      val underlying = newCCache
      val expiredEntry =
        Entry("hello", expiresAt = Some(Instant.ofEpochMilli(now.toMillis).minusSeconds(60)))
      underlying.put(MyInt(1), expiredEntry)
      newIOCache(underlying).get(MyInt(1)).map(_ shouldBe None)
    }
  }

  it should "return the value stored in the underlying cache if the value has not expired" in ticked { _ =>
    Clock[IO].monotonic.flatMap { now =>
      val underlying = newCCache
      val expiredEntry =
        Entry("hello", expiresAt = Some(Instant.ofEpochMilli(now.toMillis).plusSeconds(60)))
      underlying.put(MyInt(1), expiredEntry)
      newIOCache(underlying).get(MyInt(1)).map(_ shouldBe Some("hello"))
    }
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache with no TTL" in ticked { _ =>
    val underlying = newCCache
    newIOCache(underlying).put(MyInt(1))("hello", None) *>
      IO { underlying.getIfPresent(MyInt(1)) }
        .map(_ shouldBe Entry("hello", None))
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache with the given TTL" in ticked { implicit ticker =>
    val ctx = ticker.ctx
    val now = Instant.ofEpochMilli(ctx.now().toMillis)

    val underlying = newCCache

    newFCache[IO, String](underlying).put(MyInt(1))("hello", Some(10.seconds)).map { _ =>
      underlying.getIfPresent(MyInt(1)) should be(Entry("hello", expiresAt = Some(now.plusSeconds(10))))
    }
  }

  it should "support a TTL greater than Int.MaxValue millis" in ticked { implicit ticker =>
    val ctx = ticker.ctx
    val now = Instant.ofEpochMilli(ctx.now().toMillis)

    val underlying = newCCache
    newFCache[IO, String](underlying).put(MyInt(1))("hello", Some(30.days)).map { _ =>
      underlying.getIfPresent(MyInt(1)) should be(
        Entry("hello", expiresAt = Some(now.plusMillis(30.days.toMillis)))
      )
    }
  }

  behavior of "remove"

  it should "delete the given key and its value from the underlying cache" in ticked { _ =>
    val underlying = newCCache
    val entry      = Entry("hello", expiresAt = None)
    underlying.put(MyInt(1), entry)
    underlying.getIfPresent(MyInt(1)) should be(entry)

    newIOCache(underlying).remove(MyInt(1)) *>
      IO(underlying.getIfPresent(MyInt(1))).map(_ shouldBe null)
  }

  behavior of "get after put"

  it should "store the given key-value pair in the underlying cache with no TTL, then get it back" in ticked { _ =>
    val underlying = newCCache
    val cache      = newIOCache(underlying)
    cache.put(MyInt(1))("hello", None) *>
      cache.get(MyInt(1)).map { _ shouldBe defined }
  }

  behavior of "get after put with TTL"

  it should "store the given key-value pair with the given TTL, then get it back when not expired" in ticked { _ =>
    val underlying = newCCache
    val cache      = newFCache[IO, String](underlying)

    cache.put(MyInt(1))("hello", Some(5.seconds)) *>
      cache.get(MyInt(1)).map { _ shouldBe defined }
  }

  it should "store the given key-value pair with the given TTL, then get it back (after a sleep) when not expired" in ticked {
    _ =>
      val underlying = newCCache
      val cache      = newFCache[IO, String](underlying)

      cache.put(MyInt(1))("hello", Some(50.seconds)) *>
        IO.sleep(40.seconds) *> // sleep, but not long enough for the entry to expire
        cache.get(MyInt(1)).map { _ shouldBe defined }
  }

  it should "store the given key-value pair with the given TTL, then return None if the entry has expired" in ticked {
    _ =>
      val underlying = newCCache
      val cache      = newFCache[IO, String](underlying)

      cache.put(MyInt(1))("hello", Some(50.seconds)) *>
        IO.sleep(60.seconds) *> // sleep long enough for the entry to expire
        cache.get(MyInt(1)).map { _ shouldBe empty }
  }

}
