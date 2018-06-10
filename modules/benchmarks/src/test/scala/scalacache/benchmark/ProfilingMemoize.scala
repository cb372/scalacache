package scalacache.benchmark

import cats.effect.IO
import com.github.benmanes.caffeine.cache.Caffeine
import scalacache._
import scalacache.caffeine._
import scalacache.memoization._

/**
  * Just runs forever, endlessly calling memoize, so Java Flight Recorder can output sampling data.
  */
object ProfilingMemoize extends App {

  val underlyingCache = Caffeine.newBuilder().build[String, Entry]()
  implicit val cache = CaffeineCache[IO](underlyingCache)

  val key = "key"
  val value: String = "value"

  def itemCachedMemoize(key: String): IO[String] = memoize(None) { value }

  var result: String = _
  var i = 0L

  while (i < Long.MaxValue) {
    result = itemCachedMemoize(key).unsafeRunSync()
    i += 1
  }
  println(result)

}
