package scalacache

import scalacache.memoization.MemoizationConfig

case class CacheConfig(
    //cacheKeyBuilder: CacheKeyBuilder[K] = DefaultCacheKeyBuilder(),
    memoization: MemoizationConfig = MemoizationConfig()
)

object CacheConfig {

  implicit val defaultCacheConfig: CacheConfig = CacheConfig()

}
