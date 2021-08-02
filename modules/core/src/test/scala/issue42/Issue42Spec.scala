package issue42

import scala.util.Random
import cats.effect.SyncIO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Issue42Spec extends AnyFlatSpec with Matchers {

  case class User(id: Int, name: String)

  import scalacache._
  import memoization._

  import concurrent.duration._
  import scala.language.postfixOps

  implicit val cache: Cache[SyncIO, String, User] with MemoizingCache[SyncIO, User] = new MockCache()

  def generateNewName() = Random.alphanumeric.take(10).mkString

  def getUser(id: Int)(implicit flags: Flags): User =
    memoize(None) {
      User(id, generateNewName())
    }.unsafeRunSync()

  def getUserWithTtl(id: Int)(implicit flags: Flags): User =
    memoize(Some(1 days)) {
      User(id, generateNewName())
    }.unsafeRunSync()

  "memoize without TTL" should "respect implicit flags" in {
    val user1before = getUser(1)
    val user1after = {
      implicit val flags = Flags(readsEnabled = false)
      getUser(1)
    }
    user1before should not be user1after
  }

  "memoize with TTL" should "respect implicit flags" in {
    val user1before = getUserWithTtl(1)
    val user1after = {
      implicit val flags = Flags(readsEnabled = false)
      getUserWithTtl(1)
    }
    user1before should not be user1after
  }

}
