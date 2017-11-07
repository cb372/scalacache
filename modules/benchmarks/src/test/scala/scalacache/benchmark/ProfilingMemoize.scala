package scalacache.benchmark

import com.github.benmanes.caffeine.cache.Caffeine

import scalacache._
import scalacache.caffeine._
import scalacache.memoization._
import scalacache.modes.sync._

/**
  * Just runs forever, endlessly calling memoize, so Java Flight Recorder can output sampling data.
  */
object ProfilingMemoize extends App {

  val underlyingCache = Caffeine.newBuilder().build[String, Entry[String]]()
  implicit val cache = CaffeineCache[String](underlyingCache)

  val key = "key"
  val value: String = "value"

  def itemCachedMemoize(key: String): String = memoizeSync(None) {
    value
  }

  var result: String = _
  var i = 0L

  while (i < Long.MaxValue) {
    result = itemCachedMemoize(key)
    i += 1
  }
  println(result)

}
