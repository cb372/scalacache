package scalacache.memoization

import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.language.postfixOps
import scalacache._
import scalacache.modes.sync._

import scala.util.Try

class MemoizeSpec extends FlatSpec with Matchers {

  behavior of "memoize block"

  val expectedKey = "scalacache.memoization.MemoizeSpec.MyMockClass.myLongRunningMethod(123, abc)"

  it should "execute the block and cache the result, if there is a cache miss" in {
    implicit val emptyCache = new EmptyCache[String] with LoggingCache[String]

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClass(mockDbCall).myLongRunningMethod(123, "abc")
    result should be("hello")

    // should check the cache first
    emptyCache.getCalledWithArgs should be(Seq(expectedKey))

    // then execute the block
    mockDbCall.calledWithArgs should be(Seq(123))

    // and finally store the result in the cache
    emptyCache.putCalledWithArgs should be(Seq((expectedKey, result, None)))
  }

  it should "not execute the block if there is a cache hit" in {
    implicit val fullCache = new FullCache[String]("cache hit") with LoggingCache[String]

    val mockDbCall = new MockDbCall("hello")

    // should return the cached result
    val result = new MyMockClass(mockDbCall).myLongRunningMethod(123, "abc")
    result should be("cache hit")

    // should check the cache first
    fullCache.getCalledWithArgs should be(Seq(expectedKey))

    // should NOT execute the block
    mockDbCall.calledWithArgs should be(empty)

    // should NOT update the cache
    fullCache.putCalledWithArgs should be(empty)
  }

  it should "execute the block if cache reads are disabled" in {
    implicit val fullCache = new FullCache[String]("cache hit") with LoggingCache[String]
    implicit val flags = Flags(readsEnabled = false)

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClass(mockDbCall).myLongRunningMethod(123, "abc")
    result should be("hello")

    // should NOT check the cache, because reads are disabled
    fullCache.getCalledWithArgs should be(empty)

    // should execute the block
    mockDbCall.calledWithArgs should be(Seq(123))

    // and then store the result in the cache
    fullCache.putCalledWithArgs should be(Seq((expectedKey, result, None)))
  }

  it should "not cache the result if cache writes are disabled" in {
    implicit val emptyCache = new EmptyCache[String] with LoggingCache[String]
    implicit val flags = Flags(writesEnabled = false)

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClass(mockDbCall).myLongRunningMethod(123, "abc")
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
    implicit val emptyCache = new EmptyCache[Int] with LoggingCache[Int]
    def foo(key: Int): Int = memoizeSync(None) {
      key + 1
    }
    """ should compile
  }

  it should "catch exceptions thrown by the cache" in {
    implicit val dodgyCache = new ErrorRaisingCache[String] with LoggingCache[String]

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClass(mockDbCall).myLongRunningMethod(123, "abc")
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

    implicit val emptyCache = new EmptyCache[String] with LoggingCache[String]

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClass(mockDbCall).withTTL(123, "abc")
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

    implicit val emptyCache = new EmptyCache[String] with LoggingCache[String]
    implicit val mode = scalacache.modes.try_.mode

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClassWithTry(mockDbCall).myLongRunningMethod(123, "abc").get

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

    implicit val fullCache = new FullCache[String]("cache hit") with LoggingCache[String]
    implicit val mode = scalacache.modes.try_.mode

    val mockDbCall = new MockDbCall("hello")

    // should return the cached result
    val result = new MyMockClassWithTry(mockDbCall).myLongRunningMethod(123, "abc").get

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

    implicit val dodgyCache = new ErrorRaisingCache[String] with LoggingCache[String]
    implicit val mode = scalacache.modes.try_.mode

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClassWithTry(mockDbCall).myLongRunningMethod(123, "abc").get

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

    implicit val emptyCache = new EmptyCache[String] with LoggingCache[String]
    implicit val mode = scalacache.modes.try_.mode

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClassWithTry(mockDbCall).withTTL(123, "abc").get

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

  class MyMockClass(dbCall: Int => String)(implicit val cache: Cache[String], mode: Mode[Id], flags: Flags) {

    def myLongRunningMethod(a: Int, b: String): String = memoizeSync(None) {
      dbCall(a)
    }

    def withTTL(a: Int, b: String): String = memoizeSync(Some(10 seconds)) {
      dbCall(a)
    }

  }

  class MyMockClassWithTry(dbCall: Int => String)(implicit cache: Cache[String], mode: Mode[Try], flags: Flags) {

    def myLongRunningMethod(a: Int, b: String): Try[String] = memoizeF(None) {
      Try { dbCall(a) }
    }

    def withTTL(a: Int, b: String): Try[String] = memoizeF(Some(10 seconds)) {
      Try { dbCall(a) }
    }

  }

}
