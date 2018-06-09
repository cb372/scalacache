package scalacache

case class CacheStats(hitCount: Long, missCount: Long)

object CacheStats {

  implicit val empty: CacheStats = CacheStats(0, 0)

}
