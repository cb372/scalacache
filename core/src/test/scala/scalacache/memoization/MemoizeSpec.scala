package scalacache.memoization

import scalacache._

import org.scalatest.ShouldMatchers
import org.scalatest.FlatSpec

import scala.language.postfixOps
import scala.concurrent.duration._

/**
 * Author: chris
 * Created: 2/19/13
 */
class MemoizeSpec extends FlatSpec with ShouldMatchers {

  behavior of "memoize block"

  val expectedKey = "scalacache.memoization.MemoizeSpec.MyMockClass.myLongRunningMethod(123, abc)"

  it should "execute the block and cache the result, if there is a cache miss" in {
    val emptyCache = new EmptyCache with LoggingCache
    val cacheConfig = CacheConfig(emptyCache)

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClass(mockDbCall)(cacheConfig).myLongRunningMethod(123, "abc")
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
    val cacheConfig = CacheConfig(fullCache)

    val mockDbCall = new MockDbCall("hello")

    // should return the cached result
    val result = new MyMockClass(mockDbCall)(cacheConfig).myLongRunningMethod(123, "abc")
    result should be("cache hit")

    // should check the cache first
    fullCache.getCalledWithArgs should be(Seq(expectedKey))

    // should NOT execute the block
    mockDbCall.calledWithArgs should be(empty)

    // should NOT update the cache
    fullCache.putCalledWithArgs should be(empty)
  }

  behavior of "memoize block with TTL"

  it should "pass the TTL parameter to the cache" in {
    val expectedKey = "scalacache.memoization.MemoizeSpec.MyMockClass.withTTL(123, abc)"

    val emptyCache = new EmptyCache with LoggingCache
    val cacheConfig = CacheConfig(emptyCache)

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClass(mockDbCall)(cacheConfig).withTTL(123, "abc")
    result should be("hello")

    // should check the cache first
    emptyCache.getCalledWithArgs should be(Seq(expectedKey))

    // then execute the block
    mockDbCall.calledWithArgs should be(Seq(123))

    // and finally store the result in the cache
    emptyCache.putCalledWithArgs should be(Seq((expectedKey, result, Some(10 seconds))))
  }

  class EmptyCache extends Cache {
    def get[V](key: String): Option[V] = { None }
    def put[V](key: String, value: V, ttl: Option[Duration]) = {}
    def remove(key: String) = {}
  }

  class FullCache(value: Any) extends Cache {
    def get[V](key: String) = { Some(value).asInstanceOf[Option[V]] }
    def put[V](key: String, value: V, ttl: Option[Duration]) = {}
    def remove(key: String) = {}
  }

  class MyMockClass(dbCall: Int => String)(implicit val cacheConfig: CacheConfig) {

    def myLongRunningMethod(a: Int, b: String): String = memoize {
      dbCall(a)
    }

    def withTTL(a: Int, b: String): String = memoize(10 seconds) {
      dbCall(a)
    }

  }

}
