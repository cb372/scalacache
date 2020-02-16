package scalacache.memoization

import org.scalatest._

import scalacache._
import scalacache.modes.sync._

trait CacheKeySpecCommon extends Suite with Matchers with BeforeAndAfter {

  implicit def config: CacheConfig

  implicit lazy val cache: MockCache[Int] = new MockCache[Int]()(config)

  before {
    cache.mmap.clear()
  }

  def checkCacheKey(expectedKey: String)(call: => Int) {
    // Run the memoize block, putting some value into the cache
    val value = call

    // Check that the value is in the cache, with the expected key
    cache.get(expectedKey) should be(Some(value))
  }

  def multipleArgLists(a: Int, b: String)(c: String, d: Int): Int =
    memoizeSync {
      123
    }(_ => None)

  case class CaseClass(a: Int) { override def toString = "custom toString" }
  def takesCaseClass(cc: CaseClass): Int =
    memoizeSync {
      123
    }(_ => None)

  def lazyArg(a: => Int): Int =
    memoizeSync {
      123
    }(_ => None)

  def functionArg(a: String => Int): Int =
    memoizeSync {
      123
    }(_ => None)

  def withExcludedParams(a: Int, @cacheKeyExclude b: String, c: String)(@cacheKeyExclude d: Int): Int =
    memoizeSync {
      123
    }(_ => None)

}

class AClass(implicit cache: Cache[Int]) {
  def insideClass(a: Int): Int =
    memoizeSync {
      123
    }(_ => None)

  class InnerClass {
    def insideInnerClass(a: Int): Int =
      memoizeSync {
        123
      }(_ => None)
  }
  val inner = new InnerClass

  object InnerObject {
    def insideInnerObject(a: Int): Int =
      memoizeSync {
        123
      }(_ => None)
  }
}

trait ATrait {
  implicit val cache: Cache[Int]

  def insideTrait(a: Int): Int =
    memoizeSync {
      123
    }(_ => None)
}

object AnObject {
  implicit var cache: Cache[Int] = null
  def insideObject(a: Int): Int =
    memoizeSync {
      123
    }(_ => None)
}

class ClassWithConstructorParams(b: Int) {
  implicit var cache: Cache[Int] = null
  def foo(a: Int): Int =
    memoizeSync {
      a + b
    }(_ => None)
}

class ClassWithExcludedConstructorParam(b: Int, @cacheKeyExclude c: Int) {
  implicit var cache: Cache[Int] = null
  def foo(a: Int): Int =
    memoizeSync {
      a + b + c
    }(_ => None)
}
