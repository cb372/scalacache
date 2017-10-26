package issue42

import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

class Issue42Spec extends FlatSpec with Matchers {

  case class User(id: Int, name: String)

  import scalacache._
  import memoization._

  import concurrent.duration._
  import scala.language.postfixOps

  implicit val cache: Cache[User] = new MockCache()
  import scalacache.modes.sync._

  def generateNewName() = Random.alphanumeric.take(10).mkString

  def getUser(id: Int)(implicit flags: Flags): User = memoizeSync(None) {
    User(id, generateNewName())
  }

  def getUserWithTtl(id: Int)(implicit flags: Flags): User = memoizeSync(Some(1 days)) {
    User(id, generateNewName())
  }

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
