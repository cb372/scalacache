package scalacache.memoization

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scalacache.{ ScalaCache, MockCache }

trait CacheKeySpecCommon extends Suite with Matchers with ScalaFutures with BeforeAndAfter {

  val cache = new MockCache
  implicit def scalaCache: ScalaCache

  before {
    cache.mmap.clear()
  }

  def checkCacheKey(expectedKey: String)(call: => Int) {
    // Run the memoize block, putting some value into the cache
    val value = call

    // Check that the value is in the cache, with the expected key
    whenReady(cache.get(expectedKey)) { result =>
      result should be(Some(value))
    }
  }

  def multipleArgLists(a: Int, b: String)(c: String, d: Int): Int = memoize {
    123
  }

  case class CaseClass(a: Int) { override def toString = "custom toString" }
  def takesCaseClass(cc: CaseClass): Int = memoize {
    123
  }

  def lazyArg(a: => Int): Int = memoize {
    123
  }

  def functionArg(a: String => Int): Int = memoize {
    123
  }

  def withExcludedParams(a: Int, @cacheKeyExclude b: String, c: String)(@cacheKeyExclude d: Int): Int = memoize {
    123
  }

}

class AClass(implicit val scalaCache: ScalaCache) {
  def insideClass(a: Int): Int = memoize {
    123
  }

  class InnerClass {
    def insideInnerClass(a: Int): Int = memoize {
      123
    }
  }
  val inner = new InnerClass

  object InnerObject {
    def insideInnerObject(a: Int): Int = memoize {
      123
    }
  }
}

trait ATrait {
  implicit val scalaCache: ScalaCache

  def insideTrait(a: Int): Int = memoize {
    123
  }
}

object AnObject {
  implicit var scalaCache: ScalaCache = null
  def insideObject(a: Int): Int = memoize {
    123
  }
}

class ClassWithConstructorParams(b: Int) {
  implicit var scalaCache: ScalaCache = null
  def foo(a: Int): Int = memoize {
    a + b
  }
}

class ClassWithExcludedConstructorParam(b: Int, @cacheKeyExclude c: Int) {
  implicit var scalaCache: ScalaCache = null
  def foo(a: Int): Int = memoize {
    a + b + c
  }
}
