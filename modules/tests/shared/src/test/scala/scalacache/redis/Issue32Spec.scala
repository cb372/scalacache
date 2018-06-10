package scalacache.redis

import cats.effect.IO
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import scalacache.Cache
import scalacache.memoization._
import scalacache.serialization.binary._

case class User(id: Int, name: String)

/**
  * Test to check the sample code in issue #32 runs OK
  * (just to isolate the use of the List[User] type from the Play classloader problem)
  */
class Issue32Spec extends FlatSpec with Matchers with BeforeAndAfter with RedisTestUtil {

  assumingRedisIsRunning { (pool, _) =>
    import scalacache.CatsEffect.implicits._

    implicit val cache: Cache[IO] = RedisCache[IO](pool)

    def getUser(id: Int): IO[List[User]] = memoize(None) {
      List(User(id, "Taro"))
    }

    "memoize and Redis" should "work with List[User]" in {
      getUser(1).unsafeRunSync() should be(List(User(1, "Taro")))
      getUser(1).unsafeRunSync() should be(List(User(1, "Taro")))
    }
  }

}
