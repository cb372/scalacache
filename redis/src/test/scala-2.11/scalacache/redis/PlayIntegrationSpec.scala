package scalacache.redis

import _root_.redis.clients.jedis.JedisPool
import org.scalatest.{ FlatSpec, Matchers, TestData }
import org.scalatestplus.play.OneAppPerTest
import play.api.test.FakeApplication
import play.api.{ Application, GlobalSettings }

import scalacache._
import scalacache.memoization._

class PlayIntegrationSpec extends FlatSpec with Matchers with OneAppPerTest {

  override def newAppForTest(testData: TestData) = new FakeApplication(
    withGlobal = Some(Global)
  )

  "Redis and memoization" should "work with Play in one application" in {
    Global.getItems(List(1, 2)) should be(List(Item(1, "Chris"), Item(2, "Chris")))
    Global.getItems(List(1, 2)) should be(List(Item(1, "Chris"), Item(2, "Chris")))
  }

  "Redis and memoization" should "work with Play in another application" in {
    Global.getItems(List(1, 2)) should be(List(Item(1, "Chris"), Item(2, "Chris")))
    Global.getItems(List(1, 2)) should be(List(Item(1, "Chris"), Item(2, "Chris")))
  }

}

case class Item(id: Int, name: String)

object Global extends GlobalSettings {
  @volatile implicit var jedisPool: JedisPool = _
  @volatile implicit var scalaCache: ScalaCache[Array[Byte]] = _

  override def onStart(app: Application): Unit = {
    jedisPool = new JedisPool("localhost", 6379)
    scalaCache = ScalaCache(RedisCache(jedisPool, customClassloader = Some(app.classloader)))
  }

  override def onStop(app: Application): Unit = {
    jedisPool.destroy()
  }

  def getItems(ids: List[Int]): List[Item] = memoizeSync {
    ids map { Item(_, "Chris") }
  }
}
