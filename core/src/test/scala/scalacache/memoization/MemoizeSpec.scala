package scalacache.memoization

import org.scalatest.{ FlatSpec, Matchers }

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.language.postfixOps
import scalacache._

/**
 * Author: chris
 * Created: 2/19/13
 */
class MemoizeSpec extends FlatSpec with Matchers {

  behavior of "memoize block"

  val expectedKey = "scalacache.memoization.MemoizeSpec.MyMockClass.myLongRunningMethod(123, abc)"

  it should "execute the block and cache the result, if there is a cache miss" in {
    val emptyCache = new EmptyCache with LoggingCache
    implicit val scalaCache = ScalaCache(emptyCache)

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
    val fullCache = new FullCache("cache hit") with LoggingCache
    implicit val scalaCache = ScalaCache(fullCache)

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
    val fullCache = new FullCache("cache hit") with LoggingCache
    implicit val scalaCache = ScalaCache(fullCache)
    implicit val flags = Flags(readsEnabled = false)

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClass(mockDbCall).myLongRunningMethod(123, "abc")
    result should be("hello")

    // should NOT check the cache, because reads are disabled
    fullCache.getCalledWithArgs should be('empty)

    // should execute the block
    mockDbCall.calledWithArgs should be(Seq(123))

    // and then store the result in the cache
    fullCache.putCalledWithArgs should be(Seq((expectedKey, result, None)))
  }

  it should "not cache the result if cache writes are disabled" in {
    val emptyCache = new EmptyCache with LoggingCache
    implicit val scalaCache = ScalaCache(emptyCache)
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
    val emptyCache = new EmptyCache with LoggingCache
    implicit val scalaCache = ScalaCache(emptyCache)
    def foo(key: Int): Int = memoize {
      key + 1
    }
    """ should compile
  }

  behavior of "memoize block with TTL"

  it should "pass the TTL parameter to the cache" in {
    val expectedKey = "scalacache.memoization.MemoizeSpec.MyMockClass.withTTL(123, abc)"

    val emptyCache = new EmptyCache with LoggingCache
    implicit val scalaCache = ScalaCache(emptyCache)

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

  class MockDbCall(result: String) extends (Int => String) {
    val calledWithArgs = ArrayBuffer.empty[Int]
    def apply(a: Int): String = {
      calledWithArgs.append(a)
      result
    }
  }

  class MyMockClass(dbCall: Int => String)(implicit val scalaCache: ScalaCache, implicit val flags: Flags) {

    def myLongRunningMethod(a: Int, b: String): String = memoize {
      dbCall(a)
    }

    def withTTL(a: Int, b: String): String = memoize(10 seconds) {
      dbCall(a)
    }

  }

}
