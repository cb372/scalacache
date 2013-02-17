package com.github.cb372.cache

import scala.language.experimental.macros
import scala.reflect.macros.Context

object Cacheable {

  /**
   * Perform the given operation and memoize its result to a cache before returning it.
   * If the result is already in the cache, return it without performing the operation.
   */
  def cacheable[A](f: => A)(implicit cacheConfig: CacheConfig): A = macro Macros.cacheableImpl[A]

}

