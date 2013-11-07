package com.github.cb372.cache.guava

import org.scalatest.{ShouldMatchers, FlatSpec}
import com.google.common.cache.{CacheBuilder, Cache => GCache}

/**
 *
 * Author: c-birchall
 * Date:   13/11/07
 */
class GuavaCacheSpec extends FlatSpec with ShouldMatchers {

  def newGCache = CacheBuilder.newBuilder().build[String, Object]

  behavior of "get"

  it should "return the value stored in the underlying cache" in {
    val underlying = newGCache
    underlying.put("key1", 123: java.lang.Integer)
    GuavaCache(underlying).get("key1") should be(Some(123))
  }

  it should "return None if the given key does not exist in the underlying cache" in {
    val underlying = newGCache
    underlying.put("key1", 123: java.lang.Integer)
    GuavaCache(underlying).get("key1") should be(Some(123))
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache" in {
    val underlying = newGCache
    GuavaCache(underlying).put("key1", 123)
    underlying.getIfPresent("key1") should be(123)
  }
  
}
