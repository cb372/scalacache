package com.github.cb372.cache.guava

import com.github.cb372.cache.Cache
import com.google.common.cache.{Cache => GCache, CacheBuilder => GCacheBuilder}

/**
 * Author: chris
 * Created: 2/19/13
 */

class GuavaCache(underlying: GCache[String, Object]) extends Cache {
  def get[V](key: String) =  Option(underlying.getIfPresent(key).asInstanceOf[V])

  def put[V](key: String, value: V) {
    underlying.put(key, value.asInstanceOf[Object])
  }
}

object GuavaCache {

  def apply: GuavaCache = apply(GCacheBuilder.newBuilder().build[String, Object]())

  def apply(underlying: GCache[String, Object]): GuavaCache = new GuavaCache(underlying)

}
