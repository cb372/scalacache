package scalacache.memoization

import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}

import scalacache.serialization.InMemoryRepr
import scalacache.{MockCache, ScalaCache}

trait CacheKeySpecCommon extends Suite with Matchers with ScalaFutures with BeforeAndAfter with Eventually {

  val cache = new MockCache
  implicit def scalaCache: ScalaCache[InMemoryRepr]

  before {
    cache.mmap.clear()
  }

  def checkCacheKey(expectedKey: String)(call: => Int) {
    // Run the memoize block, putting some value into the cache
    val value = call

    // Check that the value is in the cache, with the expected key
    eventually {
      whenReady(cache.get[Int](expectedKey)) { result =>
        result should be(Some(value))
      }
    }
  }

  def multipleArgLists(a: Int, b: String)(c: String, d: Int): Int = memoizeSync {
    123
  }

  case class CaseClass(a: Int) { override def toString = "custom toString" }
  def takesCaseClass(cc: CaseClass): Int = memoizeSync {
    123
  }

  def lazyArg(a: => Int): Int = memoizeSync {
    123
  }

  def functionArg(a: String => Int): Int = memoizeSync {
    123
  }

  def withExcludedParams(a: Int, @cacheKeyExclude b: String, c: String)(@cacheKeyExclude d: Int): Int = memoizeSync {
    123
  }

}

class AClass(implicit val scalaCache: ScalaCache[InMemoryRepr]) {
  def insideClass(a: Int): Int = memoizeSync {
    123
  }

  class InnerClass {
    def insideInnerClass(a: Int): Int = memoizeSync {
      123
    }
  }
  val inner = new InnerClass

  object InnerObject {
    def insideInnerObject(a: Int): Int = memoizeSync {
      123
    }
  }
}

trait ATrait {
  implicit val scalaCache: ScalaCache[InMemoryRepr]

  def insideTrait(a: Int): Int = memoizeSync {
    123
  }
}

object AnObject {
  implicit var scalaCache: ScalaCache[InMemoryRepr] = null
  def insideObject(a: Int): Int = memoizeSync {
    123
  }
}

class ClassWithConstructorParams(b: Int) {
  implicit var scalaCache: ScalaCache[InMemoryRepr] = null
  def foo(a: Int): Int = memoizeSync {
    a + b
  }
}

class ClassWithExcludedConstructorParam(b: Int, @cacheKeyExclude c: Int) {
  implicit var scalaCache: ScalaCache[InMemoryRepr] = null
  def foo(a: Int): Int = memoizeSync {
    a + b + c
  }
}
