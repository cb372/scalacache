package sample

import scalacache._
import memoization._
import scalacache.modes.scalaFuture._

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
    implicit val cache: Cache[User] = new MockCache()

    def getUser(id: Int): Future[User] = memoizeF(None) {
      // Do DB lookup here...
      Future { User(id, s"user$id") }
    }

    def withExpiry(id: Int): Future[User] = memoizeF(Some(60 seconds)) {
      // Do DB lookup here...
      Future { User(id, s"user$id") }
    }

    def withOptionalExpiry(id: Int): Future[User] = memoizeF(Some(60 seconds)) {
      Future { User(id, s"user$id") }
    }

    def withOptionalExpiryNone(id: Int): Future[User] = memoizeF(None) {
      Future { User(id, s"user$id") }
    }

  }

}
