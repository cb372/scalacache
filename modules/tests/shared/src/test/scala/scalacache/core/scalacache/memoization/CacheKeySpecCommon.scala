package scalacache.memoization

import cats.effect.IO
import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import scalacache._
import scalacache.core.scalacache.MockCache
import scalacache.serialization.binary._

trait CacheKeySpecCommon extends Suite with Matchers with ScalaFutures with BeforeAndAfter with Eventually {

  import scalacache.CatsEffect.implicits._

  implicit def config: CacheConfig

  implicit lazy val cache: MockCache[IO] = new MockCache[IO]()

  before {
    cache.mmap.clear()
  }

  def checkCacheKey(expectedKey: String)(call: => IO[Int]) {
    // Run the memoize block, putting some value into the cache
    val value = call.unsafeRunSync()
    cache.get(expectedKey).unsafeRunSync() should be(Some(value))
  }

  def multipleArgLists(a: Int, b: String)(c: String, d: Int): IO[Int] = memoize(None) {
    123
  }

  case class CaseClass(a: Int) { override def toString = "custom toString" }
  def takesCaseClass(cc: CaseClass): IO[Int] = scalacache.memoization.memoize(None) {
    123
  }

  def lazyArg(a: => Int): IO[Int] = memoize(None) {
    123
  }

  def functionArg(a: String => Int): IO[Int] = memoize(None) {
    123
  }

  def withExcludedParams(a: Int, @cacheKeyExclude b: String, c: String)(@cacheKeyExclude d: Int): IO[Int] =
    memoize(None) {
      123
    }

}

class AClass(implicit cache: Cache[IO]) {
  def insideClass(a: Int): IO[Int] = memoize(None) {
    123
  }

  class InnerClass {
    def insideInnerClass(a: Int): IO[Int] = memoize(None) {
      123
    }
  }
  val inner = new InnerClass

  object InnerObject {
    def insideInnerObject(a: Int): IO[Int] = memoize(None) {
      123
    }
  }
}

trait ATrait {
  implicit val cache: Cache[IO]

  def insideTrait(a: Int): IO[Int] = memoize(None) {
    123
  }
}

object AnObject {
  implicit var cache: Cache[IO] = null
  def insideObject(a: Int): IO[Int] = memoize(None) {
    123
  }
}

class ClassWithConstructorParams(b: Int) {
  implicit var cache: Cache[IO] = null
  def foo(a: Int): IO[Int] = memoize(None) {
    a + b
  }
}

class ClassWithExcludedConstructorParam(b: Int, @cacheKeyExclude c: Int) {
  implicit var cache: Cache[IO] = null
  def foo(a: Int): IO[Int] = memoize(None) {
    a + b + c
  }
}
