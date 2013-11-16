package sample

import cacheable._
import scala.concurrent.duration._
import language.postfixOps

case class User(id: Int, name: String)

/**
 * Sample showing how to use cacheable.
 */
object Sample extends App {

  class UserRepository {
    implicit val cacheConfig = CacheConfig(new MockCache(), KeyGenerator.defaultGenerator)
  
    def getUser(id: Int): User = cacheable {
      // Do DB lookup here...
      User(id, s"user${id}")
    }

    def withExpiry(id: Int): User = cacheable(60 seconds) {
      // Do DB lookup here...
      User(id, s"user${id}")
    }

  }

}

