package scalacache

trait CacheKeyBuilder[K] {

  /**
    * Build a cache key by prepending the configured prefix, if there is one
    */
  def stringToCacheKey(key: K): String
}

case class DefaultCacheKeyBuilder[K](keyPrefix: Option[String] = None, separator: String = ":")
    extends CacheKeyBuilder[K] {
  override def stringToCacheKey(key: K): String = {
    keyPrefix match {
      case Some(prefix) =>
        val sb = new StringBuilder(prefix.length + separator.length + key.toString.length)
        sb.append(prefix)
        sb.append(separator)
        sb.append(key)
        sb.toString
      case None =>
        key.toString // just return the key as is
    }
  }
}
