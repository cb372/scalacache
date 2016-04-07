package foo

import org.scalatest._

/**
 * Replicating the issue reported in https://github.com/cb372/scalacache/issues/90
 */
class Issue90Spec extends FlatSpec with Matchers {

  "Issue 90" should "be fixed" in {
    import scalacache._
    import caffeine._
    import scala.concurrent.duration._

    implicit val scalaCache = ScalaCache(CaffeineCache())

    def baz(): Seq[foo.Bar] = Seq(
      Bar(1, "one"),
      Bar(2, "two")
    )
    val ttl = 1.second

    """
    def logic(): Seq[foo.Bar] = sync.cachingWithTTL("the-key")(ttl)(baz())
    """ should compile
  }

}

case class Bar(a: Int, b: String)