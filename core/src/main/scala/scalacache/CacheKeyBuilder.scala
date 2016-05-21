package scalacache

trait CacheKeyBuilder {

  /**
   * Build a String from the given parts to use as a cache key
   */
  def toCacheKey(parts: Seq[Any])(implicit cacheConfig: CacheConfig): String

  /**
   * Build a cache key by prepending the configured prefix, if there is one
   */
  def stringToCacheKey(key: String)(implicit cacheConfig: CacheConfig): String

}

object DefaultCacheKeyBuilder extends CacheKeyBuilder {

  /**
   * Build a String from the given parts, separating them using the configured separator string.
   * Prepends the prefix if there is one.
   */
  override def toCacheKey(parts: Seq[Any])(implicit cacheConfig: CacheConfig): String = {
    // Implementation note: the type of `parts` will be `WrappedArray` when called from the package object,
    // so random access is O(1).
    val sb = new StringBuilder(128)
    val separator = cacheConfig.keySeparator

    // Add the key prefix if there is one
    cacheConfig.keyPrefix match {
      case Some(prefix) =>
        sb.append(prefix)
        sb.append(separator)
      case None => // do nothing
    }

    var i = 0

    // Add all key parts except the last one, with the separator after each one
    while (i < parts.size - 1) {
      sb.append(parts(i))
      sb.append(separator)
      i += 1
    }
    // Add the final key part
    if (i < parts.size) {
      sb.append(parts(i))
    }

    sb.toString
  }

  override def stringToCacheKey(key: String)(implicit cacheConfig: CacheConfig): String = {
    cacheConfig.keyPrefix match {
      case Some(prefix) =>
        val separator = cacheConfig.keySeparator
        val sb = new StringBuilder(prefix.length + separator.length + key.length)
        sb.append(prefix)
        sb.append(separator)
        sb.append(key)
        sb.toString
      case None =>
        key // just return the key as is
    }
  }

}
