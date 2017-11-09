package scalacache.redis

import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scalacache.memoization._
import scalacache.modes.sync._
import scalacache.serialization.binary._

case class User(id: Int, name: String)

/**
  * Test to check the sample code in issue #32 runs OK
  * (just to isolate the use of the List[User] type from the Play classloader problem)
  */
class Issue32Spec extends FlatSpec with Matchers with BeforeAndAfter with RedisTestUtil {

  assumingRedisIsRunning { (pool, client) =>
    implicit val cache = RedisCache[List[User]](pool)

    def getUser(id: Int): List[User] = memoizeSync(None) {
      List(User(id, "Taro"))
    }

    "memoize and Redis" should "work with List[User]" in {
      getUser(1) should be(List(User(1, "Taro")))
      getUser(1) should be(List(User(1, "Taro")))
    }
  }

}
