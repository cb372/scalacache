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

package scalacache.memoization

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scalacache._

import cats.effect.SyncIO
import cats.effect.Sync
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.annotation.nowarn

class MemoizeSpec extends AnyFlatSpec with Matchers {

  behavior of "memoize block"

  val expectedKey = "scalacache.memoization.MemoizeSpec.MyMockClass.myLongRunningMethod(123, abc)"

  it should "execute the block and cache the result, if there is a cache miss" in {
    implicit val emptyCache: LoggingCache[SyncIO, String] = new EmptyCache[SyncIO, String]
      with LoggingCache[SyncIO, String]

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClass(mockDbCall).myLongRunningMethod(123, "abc").unsafeRunSync()
    result should be("hello")

    // should check the cache first
    emptyCache.getCalledWithArgs should be(Seq(expectedKey))

    // then execute the block
    mockDbCall.calledWithArgs should be(Seq(123))

    // and finally store the result in the cache
    emptyCache.putCalledWithArgs should be(Seq((expectedKey, result, None)))
  }

  it should "not execute the block if there is a cache hit" in {
    implicit val fullCache: LoggingCache[SyncIO, String] = new FullCache[SyncIO, String]("cache hit")
      with LoggingCache[SyncIO, String]

    val mockDbCall = new MockDbCall("hello")

    // should return the cached result
    val result = new MyMockClass(mockDbCall).myLongRunningMethod(123, "abc").unsafeRunSync()
    result should be("cache hit")

    // should check the cache first
    fullCache.getCalledWithArgs should be(Seq(expectedKey))

    // should NOT execute the block
    mockDbCall.calledWithArgs should be(empty)

    // should NOT update the cache
    fullCache.putCalledWithArgs should be(empty)
  }

  it should "execute the block if cache reads are disabled" in {
    implicit val fullCache: LoggingCache[SyncIO, String] = new FullCache[SyncIO, String]("cache hit")
      with LoggingCache[SyncIO, String]
    implicit val flags = Flags(readsEnabled = false)

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClass(mockDbCall).myLongRunningMethod(123, "abc").unsafeRunSync()
    result should be("hello")

    // should NOT check the cache, because reads are disabled
    fullCache.getCalledWithArgs should be(empty)

    // should execute the block
    mockDbCall.calledWithArgs should be(Seq(123))

    // and then store the result in the cache
    fullCache.putCalledWithArgs should be(Seq((expectedKey, result, None)))
  }

  it should "not cache the result if cache writes are disabled" in {
    implicit val emptyCache: LoggingCache[SyncIO, String] = new EmptyCache[SyncIO, String]
      with LoggingCache[SyncIO, String]
    implicit val flags = Flags(writesEnabled = false)

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClass(mockDbCall).myLongRunningMethod(123, "abc").unsafeRunSync()
    result should be("hello")

    // should check the cache first
    emptyCache.getCalledWithArgs should be(Seq(expectedKey))

    // then execute the block
    mockDbCall.calledWithArgs should be(Seq(123))

    // should NOT update the cache
    emptyCache.putCalledWithArgs should be(empty)
  }

  it should "work with a method argument called 'key'" in {
    // Reproduces https://github.com/cb372/scalacache/issues/13
    """
    implicit val emptyCache: EmptyCache[SyncIO, Int] = new EmptyCache[SyncIO, Int] with LoggingCache[SyncIO, Int]
    def foo(key: Int): SyncIO[Int] = memoize(None) {
      key + 1
    }
    """ should compile
  }

  it should "catch exceptions thrown by the cache" in {
    implicit val dodgyCache: LoggingCache[SyncIO, String] = new ErrorRaisingCache[SyncIO, String]
      with LoggingCache[SyncIO, String]

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClass(mockDbCall).myLongRunningMethod(123, "abc").unsafeRunSync()
    result should be("hello")

    // should check the cache first
    dodgyCache.getCalledWithArgs should be(Seq(expectedKey))

    // then execute the block
    mockDbCall.calledWithArgs should be(Seq(123))

    // and then store the result in the cache
    dodgyCache.putCalledWithArgs should be(Seq((expectedKey, result, None)))
  }

  behavior of "memoize block with TTL"

  it should "pass the TTL parameter to the cache" in {
    val expectedKey = "scalacache.memoization.MemoizeSpec.MyMockClass.withTTL(123, abc)"

    implicit val emptyCache: LoggingCache[SyncIO, String] = new EmptyCache[SyncIO, String]
      with LoggingCache[SyncIO, String]

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClass(mockDbCall).withTTL(123, "abc").unsafeRunSync()
    result should be("hello")

    // should check the cache first
    emptyCache.getCalledWithArgs should be(Seq(expectedKey))

    // then execute the block
    mockDbCall.calledWithArgs should be(Seq(123))

    // and finally store the result in the cache
    emptyCache.putCalledWithArgs should be(Seq((expectedKey, result, Some(10 seconds))))
  }

  behavior of "memoizeF block"

  it should "execute the block and cache the result, if there is a cache miss" in {
    val expectedKey = "scalacache.memoization.MemoizeSpec.MyMockClassWithTry.myLongRunningMethod(123, abc)"

    implicit val emptyCache: LoggingCache[SyncIO, String] = new EmptyCache[SyncIO, String]
      with LoggingCache[SyncIO, String]

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClassWithTry(mockDbCall).myLongRunningMethod(123, "abc").unsafeRunSync()

    result should be("hello")

    // should check the cache first
    emptyCache.getCalledWithArgs should be(Seq(expectedKey))

    // then execute the block
    mockDbCall.calledWithArgs should be(Seq(123))

    // and finally store the result in the cache
    emptyCache.putCalledWithArgs should be(Seq((expectedKey, result, None)))
  }

  it should "not execute the block if there is a cache hit" in {
    val expectedKey = "scalacache.memoization.MemoizeSpec.MyMockClassWithTry.myLongRunningMethod(123, abc)"

    implicit val fullCache: LoggingCache[SyncIO, String] = new FullCache[SyncIO, String]("cache hit")
      with LoggingCache[SyncIO, String]

    val mockDbCall = new MockDbCall("hello")

    // should return the cached result
    val result = new MyMockClassWithTry(mockDbCall).myLongRunningMethod(123, "abc").unsafeRunSync()

    result should be("cache hit")

    // should check the cache first
    fullCache.getCalledWithArgs should be(Seq(expectedKey))

    // should NOT execute the block
    mockDbCall.calledWithArgs should be(empty)

    // should NOT update the cache
    fullCache.putCalledWithArgs should be(empty)
  }

  it should "catch exceptions thrown by the cache" in {
    val expectedKey = "scalacache.memoization.MemoizeSpec.MyMockClassWithTry.myLongRunningMethod(123, abc)"

    implicit val dodgyCache: LoggingCache[SyncIO, String] = new ErrorRaisingCache[SyncIO, String]
      with LoggingCache[SyncIO, String]

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClassWithTry(mockDbCall).myLongRunningMethod(123, "abc").unsafeRunSync()

    result should be("hello")

    // should check the cache first
    dodgyCache.getCalledWithArgs should be(Seq(expectedKey))

    // then execute the block
    mockDbCall.calledWithArgs should be(Seq(123))

    // and then store the result in the cache
    dodgyCache.putCalledWithArgs should be(Seq((expectedKey, result, None)))
  }

  behavior of "memoizeF block with TTL"

  it should "pass the TTL parameter to the cache" in {
    val expectedKey = "scalacache.memoization.MemoizeSpec.MyMockClassWithTry.withTTL(123, abc)"

    implicit val emptyCache: LoggingCache[SyncIO, String] = new EmptyCache[SyncIO, String]
      with LoggingCache[SyncIO, String]

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClassWithTry(mockDbCall).withTTL(123, "abc").unsafeRunSync()

    result should be("hello")

    // should check the cache first
    emptyCache.getCalledWithArgs should be(Seq(expectedKey))

    // then execute the block
    mockDbCall.calledWithArgs should be(Seq(123))

    // and finally store the result in the cache
    emptyCache.putCalledWithArgs should be(Seq((expectedKey, result, Some(10 seconds))))
  }

  class MockDbCall(result: String) extends (Int => String) {
    val calledWithArgs = ArrayBuffer.empty[Int]
    def apply(a: Int): String = {
      calledWithArgs.append(a)
      result
    }
  }

  class MyMockClass[F[_]](dbCall: Int => String)(implicit
      val cache: Cache[F, String, String],
      flags: Flags
  ) {

    @nowarn
    def myLongRunningMethod(a: Int, b: String): F[String] = memoize(None) {
      dbCall(a)
    }

    @nowarn
    def withTTL(a: Int, b: String): F[String] = memoize(Some(10 seconds)) {
      dbCall(a)
    }

  }

  class MyMockClassWithTry[F[_]](dbCall: Int => String)(implicit
      cache: Cache[F, String, String],
      F: Sync[F],
      flags: Flags
  ) {

    @nowarn
    def myLongRunningMethod(a: Int, b: String): F[String] = memoizeF(None) {
      F.delay { dbCall(a) }
    }

    @nowarn
    def withTTL(a: Int, b: String): F[String] = memoizeF(Some(10 seconds)) {
      F.delay { dbCall(a) }
    }

  }

}
