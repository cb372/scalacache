package sample

import scalacache._
import memoization._

import scala.concurrent.duration._

import language.postfixOps
import cats.effect.IO

case class User(id: Int, name: String)

/**
  * Sample showing how to use ScalaCache.
  */
object Sample extends App {

  class UserRepository {
    implicit val cache: Cache[IO, String, User] = new MockCache()

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
