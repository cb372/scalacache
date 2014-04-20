package sample

import scalacache._
import memoization._

import scala.concurrent.duration._
import language.postfixOps

case class User(id: Int, name: String)

/**
 * Sample showing how to use cacheable.
 */
object Sample extends App {

  class UserRepository {
    implicit val cacheConfig = CacheConfig(new MockCache())

    def getUser(id: Int): User = memoize {
      // Do DB lookup here...
      User(id, s"user$id")
    }

    def withExpiry(id: Int): User = memoize(60 seconds) {
      // Do DB lookup here...
      User(id, s"user$id")
    }

  }

}

