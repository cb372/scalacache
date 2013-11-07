package com.github.cb372.cache

import org.scalatest.ShouldMatchers
import org.scalatest.FlatSpec

import Cacheable.cacheable
import scala.collection.mutable.ArrayBuffer

/**
 * Author: chris
 * Created: 2/19/13
 */
class CacheableSpec extends FlatSpec with ShouldMatchers {

  behavior of "cacheable"

  it should "execute the block and cache the result, if there is a cache miss" in {
    val emptyCache = new LoggingCache {
      def _get[V](key: String): Option[V] = { None }
      def _put[V](key: String, value: V): Unit = { }
    }
    val cacheConfig = CacheConfig(emptyCache, KeyGenerator.defaultGenerator)

    val mockDbCall = new MockDbCall("hello")

    // should return the block's result
    val result = new MyMockClass(mockDbCall)(cacheConfig).myLongRunningMethod(123, "abc")
    result should be("hello")

    val expectedKey = "MyMockClass.myLongRunningMethod(123, abc)"

    // should check the cache first
    emptyCache.getCalledWithArgs should be(Seq(expectedKey))

    // then execute the block
    mockDbCall.calledWithArgs should be(Seq(123))

    // and finally store the result in the cache
    emptyCache.putCalledWithArgs should be(Seq((expectedKey, result)))
  }

  it should "not execute the block if there is a cache hit" in {
    val fullCache = new LoggingCache {
      def _get[V](key: String): Option[V] = { Some("cache hit").asInstanceOf[Option[V]] }
      def _put[V](key: String, value: V): Unit = { }
    }
    val cacheConfig = CacheConfig(fullCache, KeyGenerator.defaultGenerator)

    val mockDbCall = new MockDbCall("hello")

    // should return the cached result
    val result = new MyMockClass(mockDbCall)(cacheConfig).myLongRunningMethod(123, "abc")
    result should be("cache hit")

    val expectedKey = "MyMockClass.myLongRunningMethod(123, abc)"

    // should check the cache first
    fullCache.getCalledWithArgs should be(Seq(expectedKey))

    // should NOT execute the block
    mockDbCall.calledWithArgs should be(empty)

    // should NOT update the cache
    fullCache.putCalledWithArgs should be(empty)
  }

  trait LoggingCache extends Cache {
    var (getCalledWithArgs, putCalledWithArgs) = (ArrayBuffer.empty[String], ArrayBuffer.empty[(String, Any)])

    def get[V](key: String): Option[V] = {
      getCalledWithArgs.append(key)
      _get(key)
    }

    def put[V](key: String, value: V): Unit = {
      putCalledWithArgs.append((key, value))
      _put(key, value)
    }

    def _get[V](key: String): Option[V]
    def _put[V](key: String, value: V): Unit
  }

  class MockDbCall(result: String) extends (Int => String) {
    val calledWithArgs = ArrayBuffer.empty[Int]
    def apply(a: Int): String = {
      calledWithArgs.append(a)
      result
    }
  }

  class MyMockClass(dbCall: Int => String)(implicit val cacheConfig: CacheConfig) {

    def myLongRunningMethod(a: Int, b: String): String = {
      cacheable {
        dbCall(a)
      }
    }

  }

}
