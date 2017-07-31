package scalacache

/**
 * Configuration options for ScalaCache
 * @param keyPrefix
 *                  A global prefix that should be prepended to all cache keys.
 *                  Useful for namespacing if you are sharing your cache with another application.
 * @param keySeparator
 *                     The value used to separate different parts of a cache key
 * @param waitForWriteToComplete
 *                               If true, the `Future` returned by `caching` (or `memoize`) will not complete
 *                               until the cache write has completed.
 *                               If false, the `Future` will complete as soon as the value has been computed,
 *                               and the cache write will happen asynchronously.
 *                               The latter was the behaviour until ScalaCache 0.9.2,
 *                               but the former is more useful in many situations.
 *
 */
case class CacheConfig(
  keyPrefix: Option[String] = None,
  keySeparator: String = ":",
  waitForWriteToComplete: Boolean = true)
