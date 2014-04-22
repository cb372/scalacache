package scalacache

/**
 * Configuration options for ScalaCache
 * @param keyPrefix
 *                  A global prefix that should be prepended to all cache keys.
 *                  Useful for namespacing if you are sharing your cache with another application.
 * @param keySeparator
 *                     The value used to separate different parts of a cache key
 *
 */
case class CacheConfig(
  keyPrefix: Option[String] = None,
  keySeparator: String = ":")
