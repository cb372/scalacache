package scalacache.memoization

import org.scalatest._
import scalacache._
import scalacache.memoization.MethodCallToStringConvertor.defaultConvertor

/**
 *
 * Author: c-birchall
 * Date:   13/11/07
 */
class CacheKeySpec extends FlatSpec with ShouldMatchers with BeforeAndAfter {

  behavior of "cache key generation for method memoization"

  val cache = new MockCache
  implicit val cacheConfig = CacheConfig(cache, MemoizationConfig(defaultConvertor))

  before {
    cache.mmap.clear()
  }

  it should "include values of all arguments for all argument lists" in {
    checkCacheKey("scalacache.memoization.CacheKeySpec.multipleArgLists(1, 2)(3, 4)") {
      multipleArgLists(1, "2")("3", 4)
    }
  }

  it should "call toString on arguments to convert them into a string" in {
    checkCacheKey("scalacache.memoization.CacheKeySpec.takesCaseClass(custom toString)") {
      takesCaseClass(CaseClass(1))
    }
  }

  it should "include values of lazy arguments" in {
    checkCacheKey("scalacache.memoization.CacheKeySpec.lazyArg(1)") {
      lazyArg(1)
    }
  }

  it should "include function arguments as <functionN>" in {
    checkCacheKey("scalacache.memoization.CacheKeySpec.functionArg(<function1>)") {
      functionArg((s: String) => s.toInt + 1)
    }
  }

  it should "work for a method inside a class" in {
    checkCacheKey("scalacache.memoization.AClass.insideClass(1)") {
      new AClass().insideClass(1)
    }
  }

  it should "work for a method inside a trait" in {
    checkCacheKey("scalacache.memoization.ATrait.insideTrait(1)") {
      new ATrait { val cacheConfig = CacheKeySpec.this.cacheConfig }.insideTrait(1)
    }
  }

  it should "work for a method inside an object" in {
    AnObject.cacheConfig = this.cacheConfig
    checkCacheKey("scalacache.memoization.AnObject.insideObject(1)") {
      AnObject.insideObject(1)
    }
  }

  it should "work for a method inside a class inside a class" in {
    checkCacheKey("scalacache.memoization.AClass.InnerClass.insideInnerClass(1)") {
      new AClass().inner.insideInnerClass(1)
    }
  }

  it should "work for a method inside an object inside a class" in {
    checkCacheKey("scalacache.memoization.AClass.InnerObject.insideInnerObject(1)") {
      new AClass().InnerObject.insideInnerObject(1)
    }
  }

  it should "work for a method inside a package object" in {
    pkg.cacheConfig = this.cacheConfig
    checkCacheKey("scalacache.memoization.pkg.package.insidePackageObject(1)") {
      pkg.insidePackageObject(1)
    }
  }

  def checkCacheKey(expectedKey: String)(call: => Int) {
    // Run the memoize block, putting some value into the cache
    val value = call

    // Check that the value is in the cache, with the expected key
    cache.get(expectedKey) should be(Some(value))
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

}

class AClass(implicit val cacheConfig: CacheConfig) {
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
  implicit val cacheConfig: CacheConfig

  def insideTrait(a: Int): Int = memoize {
    123
  }
}

object AnObject {
  implicit var cacheConfig: CacheConfig = null
  def insideObject(a: Int): Int = memoize {
    123
  }
}