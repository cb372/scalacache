package integrationtests
import com.dimafeng.testcontainers.{ForAllTestContainer, GenericContainer}
import net.spy.memcached.{AddrUtil, MemcachedClient}
import org.scalatest.{FlatSpec, Matchers}
import scalacache.memcached.MemcachedCache

class MemcachedIntegrationSpec extends FlatSpec with Matchers with ForAllTestContainer with CacheBehaviours {
  private final val memcachedPort = 11211
  override val container = GenericContainer("memcached:alpine", Seq(memcachedPort))
  private var client: MemcachedClient = _

  override def afterStart(): Unit = {
    client = new MemcachedClient(
      AddrUtil.getAddresses(s"${container.containerIpAddress}:${container.mappedPort(memcachedPort)}"))
  }

  override def beforeStop(): Unit = {
    client.shutdown()
  }

  it should behave like cacheWithDifferentEffects[MemcachedClient]("(Memcached) ⇔ (binary codec)", {
    import scalacache.serialization.binary._
    MemcachedCache[String](client)
  })

  it should behave like cacheWithDifferentEffects[MemcachedClient]("(Memcached) ⇔ (circe codec)", {
    import scalacache.serialization.circe._
    MemcachedCache[String](client)
  })
}
