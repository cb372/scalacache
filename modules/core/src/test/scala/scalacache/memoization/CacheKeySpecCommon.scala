package scalacache.memoization

import org.scalatest._

import scalacache._
import cats.effect.SyncIO
import org.scalatest.matchers.should.Matchers

trait CacheKeySpecCommon extends Suite with Matchers with BeforeAndAfter {

  implicit def config: MemoizationConfig

  implicit lazy val cache: MockCache[SyncIO, Int] = new MockCache()

  before {
    cache.mmap.clear()
  }

  def checkCacheKey(expectedKey: String)(call: => Int): Unit = {
    // Run the memoize block, putting some value into the cache
    val value = call

    // Check that the value is in the cache, with the expected key
    cache.get(expectedKey).unsafeRunSync() should be(Some(value))
  }

  def multipleArgLists(a: Int, b: String)(c: String, d: Int): Int =
    memoize(None) {
      123
    }.unsafeRunSync()

  case class CaseClass(a: Int) { override def toString = "custom toString" }

  def takesCaseClass(cc: CaseClass): SyncIO[Int] = memoize(None) {
    123
  }

  def lazyArg(a: => Int): SyncIO[Int] = memoize(None) {
    123
  }

  def functionArg(a: String => Int): SyncIO[Int] = memoize(None) {
    123
  }

  def withExcludedParams(a: Int, @cacheKeyExclude b: String, c: String)(@cacheKeyExclude d: Int): SyncIO[Int] =
    memoize(None) {
      123
    }

}

class AClass[F[_]](implicit cache: Cache[F, String, Int] with MemoizingCache[F, Int]) {
  def insideClass(a: Int): F[Int] = memoize(None) {
    123
  }

  class InnerClass {
    def insideInnerClass(a: Int): F[Int] = memoize(None) {
      123
    }
  }
  val inner = new InnerClass

  object InnerObject {
    def insideInnerObject(a: Int): F[Int] = memoize(None) {
      123
    }
  }
}

trait ATrait[F[_]] {
  implicit val cache: Cache[F, String, Int] with MemoizingCache[F, Int]

  def insideTrait(a: Int): F[Int] = memoize(None) {
    123
  }
}

object AnObject {
  implicit var cache: Cache[SyncIO, String, Int] with MemoizingCache[SyncIO, Int] = null
  def insideObject(a: Int): SyncIO[Int] = memoize(None) {
    123
  }
}

class ClassWithConstructorParams[F[_]](b: Int) {
  implicit var cache: Cache[F, String, Int] with MemoizingCache[F, Int] = null
  def foo(a: Int): F[Int] = memoize(None) {
    a + b
  }
}

class ClassWithExcludedConstructorParam[F[_]](b: Int, @cacheKeyExclude c: Int) {
  implicit var cache: Cache[F, String, Int] with MemoizingCache[F, Int] = null
  def foo(a: Int): F[Int] = memoize(None) {
    a + b + c
  }
}
