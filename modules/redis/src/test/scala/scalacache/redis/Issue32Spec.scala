package scalacache.redis

import org.scalatest.BeforeAndAfter

import scalacache.Cache
import scalacache.memoization._
import scalacache.serialization.binary._
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

case class User(id: Int, name: String)

/** Test to check the sample code in issue #32 runs OK (just to isolate the use of the List[User] type from the Play
  * classloader problem)
  */
class Issue32Spec extends AnyFlatSpec with Matchers with BeforeAndAfter with RedisTestUtil {

  assumingRedisIsRunning { (pool, client) =>
    implicit val cache: Cache[IO, List[User]] = RedisCache[IO, List[User]](pool)

    def getUser(id: Int): List[User] =
      memoize(None) {
        List(User(id, "Taro"))
      }.unsafeRunSync()

    "memoize and Redis" should "work with List[User]" in {
      getUser(1) should be(List(User(1, "Taro")))
      getUser(1) should be(List(User(1, "Taro")))
    }
  }

}
