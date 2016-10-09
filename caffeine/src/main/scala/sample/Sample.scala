package sample

//import cats.Id

import cats.Id

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.language.implicitConversions
import scala.util.{ Success, Try }
import scalacache._
import scalacache.caffeine._

object Sample extends App {

  implicit val sc = ScalaCache(CaffeineCache())

  // TODO use natural transforms to support passing plain values, Try and Future to `caching`?
  {
    import scalacache.modes.throwExceptions
    put("throwExceptions")("hello")
    val value: Option[String] = typed[String, NoSerialization].get("throwExceptions")
    println(s"Plain value: $value")
  }

  {
    import scalacache.modes.wrapWithTry

    val value: Try[String] = caching("wrapWithTry")(ttl = None) {
      // TODO handle this: Success("hello")
      Try("hello")
    }
    println(s"Wrapped with Try: $value")
  }

  {
    import scala.concurrent.ExecutionContext.Implicits.global
    import scalacache.modes.runAsFuture
    val f: Future[Unit] = put("foo", "bar")("hello")
    val f2 = f.flatMap(_ => get("foo", "bar"))
    println(Await.result(f2, Duration.Inf))
    val f3: Future[String] = caching("key")(None) {
      println("Sleeping for 5 seconds")
      Thread.sleep(5000L)
      Future.successful("hello")
    }
    println("Spawned future")
    println(Await.result(f3, Duration.Inf))
  }

}
