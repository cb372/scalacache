package scalacache

trait CacheKeyBuilder {

  /**
   * Build a String from the given parts to use as a cache key
   */
  def toCacheKey(parts: Seq[Any])(implicit cacheConfig: CacheConfig): String

}

object DefaultCacheKeyBuilder extends CacheKeyBuilder {

  /**
   * Build a String from the given parts, separating them using the configured separator string.
   * Prepends the prefix if there is one.
   */
  override def toCacheKey(parts: Seq[Any])(implicit cacheConfig: CacheConfig): String = {
    val prefix = cacheConfig.keyPrefix.map(p => s"$p${cacheConfig.keySeparator}") getOrElse ""
    s"$prefix${parts.mkString(cacheConfig.keySeparator)}"
  }

}
