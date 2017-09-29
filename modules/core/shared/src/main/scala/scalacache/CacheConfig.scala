package scalacache

import scalacache.memoization.MemoizationConfig

case class CacheConfig(
  cacheKeyBuilder: CacheKeyBuilder = DefaultCacheKeyBuilder(),
  memoization: MemoizationConfig = MemoizationConfig())
