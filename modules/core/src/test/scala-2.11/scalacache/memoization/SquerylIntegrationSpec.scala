package scalacache.memoization

import java.util.Date

import org.scalatest.concurrent.{ Eventually, IntegrationPatience }
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }
import org.squeryl.adapters.H2Adapter
import org.squeryl.annotations.Column
import org.squeryl._
import org.squeryl.PrimitiveTypeMode._

import scalacache.{ LoggingCache, MockCache, ScalaCache }

/**
 * Test for https://github.com/cb372/scalacache/issues/14
 */
class SquerylIntegrationSpec extends FlatSpec with Matchers with BeforeAndAfterAll with Eventually with IntegrationPatience {
  var theUserId: Int = -1

  override def beforeAll() = {
    Class.forName("org.h2.Driver")
    SessionFactory.concreteFactory =
      Some(() => Session.create(java.sql.DriverManager.getConnection("jdbc:h2:mem:test"), new H2Adapter))
    SessionFactory.externalTransactionManagementAdapter =
      Some(() => Some(Session.create(java.sql.DriverManager.getConnection("jdbc:h2:mem:test"), new H2Adapter)))

    inTransaction {
      // create the DB table
      FooDb.create

      // Add a user to the DB
      val insertedUser = FooDb.users.insert(new User("chris", 123))
      theUserId = insertedUser.id
      println(s"Inserted user with ID $theUserId")
    }
  }

  it should "work with Squeryl" in {
    import FooDb.users
    val cache = new MockCache with LoggingCache
    implicit val scalaCache = ScalaCache(cache)

    def findUser(userId: Int): Option[User] = memoizeSync {
      inTransaction {
        from(users)((u) =>
          select(u)
        )
      }.headOption
    }

    // Check the value returned from the DB
    val fromDb = findUser(theUserId)
    fromDb should be('defined)
    fromDb.get.id should be(theUserId)
    fromDb.get.name should be("chris")
    fromDb.get.status should be(123)

    // Check that the value was (eventually) asynchronously inserted into the cache
    eventually {
      cache.putCalledWithArgs should have size 1
    }
    val cachedValue = cache.putCalledWithArgs(0)._2.asInstanceOf[Option[User]]
    cachedValue should be('defined)
    cachedValue.get.id should be(theUserId)
    cachedValue.get.name should be("chris")

    // Update the value in the DB
    FooDb.users.update(new User(theUserId, "new name", 456, fromDb.get.createdAt))

    // Call findUser again - it should return the old value from the cache
    val fromCache = findUser(theUserId)
    fromCache should be('defined)
    fromCache.get.id should be(theUserId)
    fromCache.get.name should be("chris")
    fromCache.get.status should be(123)
  }

}

case class UserStatus(dbval: Int)

class User(val id: Int = 0,
           val name: String,
           val status: Int,
           @Column("created_at") var createdAt: Date) extends KeyedEntity[Int] {

  def this() = {
    this(0, "", 0, new Date())
  }

  def this(name: String, status: Int) = {
    this(0, name, status, new Date())
  }

  def this(name: String, status: UserStatus) = {
    this(0, name, status.dbval, new Date())
  }
}

object FooDb extends Schema {
  val users: Table[User] = table[User]("user")
}

