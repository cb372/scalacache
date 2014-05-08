package scalacache.memoization

import scalacache._

import org.scalatest.ShouldMatchers
import org.scalatest.FlatSpec

import scala.language.postfixOps
import scala.concurrent.duration._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Author: chris
 * Created: 2/19/13
 */
class MemoizeSpec extends FlatSpec with ShouldMatchers {

  behavior of "memoize block"

  val expectedKey = "scalacache.memoization.MemoizeSpec.MyMockClass.myLongRunningMethod(123, abc)"

  it should "execute the block and cache the result, if there is a cache miss" in {
    val emptyCache = new EmptyCache with LoggingCache
    val scalaCache = ScalaCache(emptyCache)

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClass(mockDbCall)(scalaCache).myLongRunningMethod(123, "abc")
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
    val scalaCache = ScalaCache(fullCache)

    val mockDbCall = new MockDbCall("hello")

    // should return the cached result
    val result = new MyMockClass(mockDbCall)(scalaCache).myLongRunningMethod(123, "abc")
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
    val scalaCache = ScalaCache(emptyCache)

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClass(mockDbCall)(scalaCache).withTTL(123, "abc")
    result should be("hello")

    // should check the cache first
    emptyCache.getCalledWithArgs should be(Seq(expectedKey))

    // then execute the block
    mockDbCall.calledWithArgs should be(Seq(123))

    // and finally store the result in the cache
    emptyCache.putCalledWithArgs should be(Seq((expectedKey, result, Some(10 seconds))))
  }

  class EmptyCache extends Cache {
    override def get[V](key: String): Future[Option[V]] = Future.successful(None)
    override def put[V](key: String, value: V, ttl: Option[Duration]) = Future.successful((): Unit)
    override def remove(key: String) = Future.successful((): Unit)
  }

  class FullCache(value: Any) extends Cache {
    override def get[V](key: String): Future[Option[V]] = Future.successful(Some(value).asInstanceOf[Option[V]])
    override def put[V](key: String, value: V, ttl: Option[Duration]) = Future.successful((): Unit)
    override def remove(key: String) = Future.successful((): Unit)
  }

  class MockDbCall(result: String) extends (Int => String) {
    val calledWithArgs = ArrayBuffer.empty[Int]
    def apply(a: Int): String = {
      calledWithArgs.append(a)
      result
    }
  }

  class MyMockClass(dbCall: Int => String)(implicit val scalaCache: ScalaCache) {

    def myLongRunningMethod(a: Int, b: String): String = memoize {
      dbCall(a)
    }

    def withTTL(a: Int, b: String): String = memoize(10 seconds) {
      dbCall(a)
    }

  }

}
