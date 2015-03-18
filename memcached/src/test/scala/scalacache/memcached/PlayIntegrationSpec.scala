package scalacache.memcached

import net.spy.memcached._
import org.scalatest.{ FlatSpec, Matchers, TestData }
import org.scalatestplus.play.OneAppPerTest
import play.api.test.FakeApplication
import play.api.{ Application, GlobalSettings }

import scala.util.Random
import scalacache._
import scalacache.memoization._

class PlayIntegrationSpec extends FlatSpec with Matchers with OneAppPerTest {

  override def newAppForTest(testData: TestData) = new FakeApplication(
    withGlobal = Some(Global)
  )

  "Memcached and memoization" should "work with Play in one application" in {
    val firstResult = Global.getItems(List(1, 2))
    val secondResult = Global.getItems(List(1, 2))
    secondResult should be(firstResult)
  }

  "Memcached and memoization" should "work with Play in another application" in {
    val firstResult = Global.getItems(List(1, 2))
    val secondResult = Global.getItems(List(1, 2))
    secondResult should be(firstResult)
  }

}

case class Item(id: Int, name: String)

object Global extends GlobalSettings {
  @volatile implicit var client: MemcachedClient = _
  @volatile implicit var scalaCache: ScalaCache = _

  override def onStart(app: Application): Unit = {
    client = new MemcachedClient(new BinaryConnectionFactory(), AddrUtil.getAddresses("localhost:11211"))
    scalaCache = ScalaCache(MemcachedCache(client, customClassloader = Some(app.classloader)))
  }

  override def onStop(app: Application): Unit = {
    client.shutdown()
  }

  def getItems(ids: List[Int]): List[Item] = memoize {
    ids map { _ => Item(Random.nextInt(), "Chris") }
  }

}
