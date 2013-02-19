package com.github.cb372.cache

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.scalamock.scalatest.MockFactory

import Cacheable.cacheable
import org.scalamock.MockParameter

/**
 * Author: chris
 * Created: 2/19/13
 */
class CacheableSpec extends FlatSpec with ShouldMatchers with MockFactory {

  behavior of "cacheable"

  it should "execute the block and cache the result, if there is a cache miss" in {
    val emptyCache = stub[Cache]
    (emptyCache.get _).when(*).returns(None)
    (emptyCache.put _).when(*, *).returns(Unit)

    val cacheConfig = CacheConfig(emptyCache, KeyGenerator.defaultGenerator)

    val mockDbCall = stub[Function0[String]]
    (mockDbCall.apply _).when().returns("hello")

    // should return the block's result
    val result = new MyMockClass(mockDbCall)(cacheConfig).myLongRunningMethod(123, "abc")
    result should be("hello")

    val expectedKey = "MyMockClass.myLongRunningMethod(123, abc)"

    inSequence {
      // should check the cache first
      (emptyCache.get _).verify(expectedKey).once()

      // then execute the block
      (mockDbCall.apply _).verify().once()

      // and finally store the result in the cache
      (emptyCache.put[String] _).verify(expectedKey, result).once()
    }

  }

  it should "not execute the block if there is a cache hit" in {
    val fullCache = stub[Cache]
    (fullCache.get[String] _).when(*).returns(Some("cache hit"))

    val cacheConfig = CacheConfig(fullCache, KeyGenerator.defaultGenerator)

    val mockDbCall = stub[Function0[String]]

    // should return the cached result
    val result = new MyMockClass(mockDbCall)(cacheConfig).myLongRunningMethod(123, "abc")
    result should be("cache hit")

    val expectedKey = "MyMockClass.myLongRunningMethod(123, abc)"

    // should check the cache first
    (fullCache.get _).verify(expectedKey).once()

    // should NOT execute the block
    (mockDbCall.apply _).verify().never()

    // should NOT update the cache
    (fullCache.put[String] _).verify(expectedKey, result).never()
  }

  class MyMockClass(dbCall: () => String)(implicit val cacheConfig: CacheConfig) {

    def myLongRunningMethod(a: Int, b: String): String = {
      cacheable {
        dbCall()
      }
    }

  }

}
