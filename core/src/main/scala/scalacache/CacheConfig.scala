package scalacache

import scalacache.memoization.{ MemoizationConfig, MethodCallToStringConvertor }

/**
 * Configuration to be used when interacting with a cache.
 * @param cache The cache itself
 * @param memoization Configuration related to method memoization
 */
case class CacheConfig(
  cache: Cache,
  memoization: MemoizationConfig = MemoizationConfig())

