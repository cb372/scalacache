package com.github.cb372.cache.guava

import com.github.cb372.cache.Cache
import com.google.common.cache.{Cache => GCache, CacheBuilder => GCacheBuilder}
import scala.concurrent.duration.Duration
import org.joda.time.DateTime

/**
 * Author: chris
 * Created: 2/19/13
 */

/*
Note: Would be nice to use Any here, but that doesn't conform to GCache's type bounds,
because Any does not extend java.lang.Object.
 */
class GuavaCache(underlying: GCache[String, Object]) extends Cache {

  def get[V](key: String) =  {
    val entry = Option(underlying.getIfPresent(key).asInstanceOf[Entry[V]])
    /*
     Note: we could delete the entry from the cache if it is expired,
     but that would lead to nasty race conditions in case of concurrent access.
     We might end up deleting an entry that another thread has just inserted.
     */
    entry.flatMap { e =>
      if (e.isExpired) None
      else Some(e.value)
    }
  }

  def put[V](key: String, value: V, ttl: Option[Duration]) {
    val entry = Entry(value, ttl.map(toExpiryTime))
    underlying.put(key, entry.asInstanceOf[Object])
  }

  private def toExpiryTime(ttl: Duration): DateTime = DateTime.now.plusMillis(ttl.toMillis.toInt)

}

object GuavaCache {

  /**
   * Create a new Guava cache
   */
  def apply: GuavaCache = apply(GCacheBuilder.newBuilder().build[String, Object]())

  /**
   * Create a new cache utilizing the given underlying Guava cache.
   * @param underlying a Guava cache
   */
  def apply(underlying: GCache[String, Object]): GuavaCache = new GuavaCache(underlying)

}
