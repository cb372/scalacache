package cacheable

/**
 * Configuration to be used when interacting with a cache.
 * @param cache The cache itself
 * @param keyGenerator The cache key generator
 */
case class CacheConfig (
  cache: Cache,
  keyGenerator: KeyGenerator = KeyGenerator.defaultGenerator
)

