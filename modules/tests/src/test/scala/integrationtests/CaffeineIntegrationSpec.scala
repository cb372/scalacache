package integrationtests

import org.scalatest.{FlatSpec, Matchers}
import scalacache.caffeine.CaffeineCache

class CaffeineIntegrationSpec extends FlatSpec with Matchers with CacheBehaviours {

  it should behave like cacheWithDifferentEffects[Unit]("Caffeine", CaffeineCache[String])

}
