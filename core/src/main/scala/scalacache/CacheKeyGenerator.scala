package scalacache

/**
 *
 * Author: c-birchall
 * Date:   2014/04/22
 */
trait CacheKeyGenerator {

  /**
   * Build a String from the given parts to use as a cache key
   */
  def toCacheKey(parts: Any*)(implicit cacheConfig: CacheConfig): String

}

object DefaultCacheKeyGenerator extends CacheKeyGenerator {

  /**
   * Build a String from the given parts, separating them using the configured separator string.
   * Prepends the prefix if there is one.
   */
  override def toCacheKey(parts: Any*)(implicit cacheConfig: CacheConfig): String = {
    val prefix = cacheConfig.keyPrefix.map(p => s"$p${cacheConfig.keySeparator}") getOrElse ""
    s"$prefix${parts.mkString(cacheConfig.keySeparator)}"
  }

}
