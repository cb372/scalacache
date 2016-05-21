package scalacache.benchmark

import com.github.benmanes.caffeine.cache.Caffeine

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalacache._
import scalacache.caffeine._
import scalacache.memoization._

/**
  * Just runs forever, endlessly calling memoize, so Java Flight Recorder can output sampling data.
  */
object ProfilingMemoize extends App {

  val underlyingCache = Caffeine.newBuilder().build[String, Object]()
  implicit val scalaCache = ScalaCache(CaffeineCache(underlyingCache))
  val typedCache = typed[String, NoSerialization]

  val key = "key"
  val value: String = "value"

  def itemCachedMemoize(key: String): Future[String] = memoize {
    Future.successful(value)
  }

  // populate the cache
  put(key)(value)

  var result: String = _
  var i = 0L

  while(i < Long.MaxValue) {
    result = Await.result(itemCachedMemoize(key), Duration.Inf)
    i += 1
  }
  println(result)

}
