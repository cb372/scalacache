package sample

import scalacache._
import memoization._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import language.postfixOps

case class User(id: Int, name: String)

/**
  * Sample showing how to use ScalaCache.
  */
object Sample extends App {

  class UserRepository {
    implicit val cacheConfig = ScalaCache(new MockCache())

    def getUser(id: Int): Future[User] = memoize {
      // Do DB lookup here...
      Future { User(id, s"user$id") }
    }

    def withExpiry(id: Int): Future[User] = memoize(60 seconds) {
      // Do DB lookup here...
      Future { User(id, s"user$id") }
    }

    def withOptionalExpiry(id: Int): Future[User] = memoize(Option(60 seconds)) {
      Future { User(id, s"user$id") }
    }

    def withOptionalExpiryNone(id: Int): Future[User] = memoize(None) {
      Future { User(id, s"user$id") }
    }

  }

}
