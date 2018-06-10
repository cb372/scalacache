package scalacache.core.sample

import cats.effect.IO
import scalacache._
import scalacache.core.scalacache.MockCache
import scalacache.memoization._

import scala.concurrent.duration._
import scala.language.postfixOps

case class User(id: Int, name: String)

/**
  * Sample showing how to use ScalaCache.
  */
object Sample extends App {

  class UserRepository {

    import scalacache.CatsEffect.implicits._
    import scalacache.serialization.binary._

    implicit val cache: Cache[IO] = new MockCache()

    def getUser(id: Int): IO[User] = memoizeF(None) {
      // Do DB lookup here...
      IO { User(id, s"user$id") }
    }

    def withExpiry(id: Int): IO[User] = memoizeF(Some(60 seconds)) {
      // Do DB lookup here...
      IO { User(id, s"user$id") }
    }

    def withOptionalExpiry(id: Int): IO[User] = memoizeF(Some(60 seconds)) {
      IO { User(id, s"user$id") }
    }

    def withOptionalExpiryNone(id: Int): IO[User] = memoizeF(None) {
      IO { User(id, s"user$id") }
    }

  }

}
