package scalacache.memoization

import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import scalacache._

import scala.concurrent.Future

trait CacheKeySpecCommon extends Suite with Matchers with ScalaFutures with BeforeAndAfter with Eventually {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scalacache.modes.scalaFuture.mode
  import scalacache.serialization.binary._

  implicit def config: CacheConfig

  implicit lazy val cache: MockCache[Future] = new MockCache[Future]()

  before {
    cache.mmap.clear()
  }

  def checkCacheKey(expectedKey: String)(call: => Future[Int]) {
    // Run the memoize block, putting some value into the cache
    val future = call

    whenReady(future) { value =>
      // Check that the value is in the cache, with the expected key
      eventually {
        cache.get(expectedKey) should be(Some(value))
      }
    }
  }

  def multipleArgLists(a: Int, b: String)(c: String, d: Int): Future[Int] = memoize(None) {
    123
  }

  case class CaseClass(a: Int) { override def toString = "custom toString" }
  def takesCaseClass(cc: CaseClass): Future[Int] = scalacache.memoization.memoize(None) {
    123
  }

  def lazyArg(a: => Int): Future[Int] = memoize(None) {
    123
  }

  def functionArg(a: String => Int): Future[Int] = memoize(None) {
    123
  }

  def withExcludedParams(a: Int, @cacheKeyExclude b: String, c: String)(@cacheKeyExclude d: Int): Future[Int] =
    memoize(None) {
      123
    }

}

class AClass(implicit cache: Cache[Future]) {
  import scalacache.serialization.binary._

  def insideClass(a: Int): Future[Int] = memoize(None) {
    123
  }

  class InnerClass {
    def insideInnerClass(a: Int): Future[Int] = memoize(None) {
      123
    }
  }
  val inner = new InnerClass

  object InnerObject {
    def insideInnerObject(a: Int): Future[Int] = memoize(None) {
      123
    }
  }
}

trait ATrait {
  import scalacache.serialization.binary._

  implicit val cache: Cache[Future]

  def insideTrait(a: Int): Future[Int] = memoize(None) {
    123
  }
}

object AnObject {
  import scalacache.serialization.binary._

  implicit var cache: Cache[Future] = null
  def insideObject(a: Int): Future[Int] = memoize(None) {
    123
  }
}

class ClassWithConstructorParams(b: Int) {
  import scalacache.serialization.binary._

  implicit var cache: Cache[Future] = null
  def foo(a: Int): Future[Int] = memoize(None) {
    a + b
  }
}

class ClassWithExcludedConstructorParam(b: Int, @cacheKeyExclude c: Int) {
  import scalacache.serialization.binary._

  implicit var cache: Cache[Future] = null
  def foo(a: Int): Future[Int] = memoize(None) {
    a + b + c
  }
}
