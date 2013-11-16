package cacheable

import org.scalatest._

/**
 *
 * Author: c-birchall
 * Date:   13/11/07
 */
class CacheKeySpec extends FlatSpec with ShouldMatchers with BeforeAndAfter {

  behavior of "cache key generation"

  val cache = new MockCache
  implicit val cacheConfig = CacheConfig(cache, KeyGenerator.defaultGenerator)

  before {
    cache.mmap.clear()
  }

  it should "include values of all arguments for all argument lists" in {
    checkCacheKey("cacheable.CacheKeySpec.multipleArgLists(1, 2)(3, 4)") {
      multipleArgLists(1, "2")("3", 4)
    }
  }

  it should "call toString on arguments to convert them into a string" in {
    checkCacheKey("cacheable.CacheKeySpec.takesCaseClass(custom toString)") {
      takesCaseClass(CaseClass(1))
    }
  }

  it should "include values of lazy arguments" in {
    checkCacheKey("cacheable.CacheKeySpec.lazyArg(1)") {
      lazyArg(1)
    }
  }

  it should "include function arguments as <functionN>" in {
    checkCacheKey("cacheable.CacheKeySpec.functionArg(<function1>)") {
      functionArg((s: String) => s.toInt + 1)
    }
  }

  it should "work for a method inside a class" in {
    checkCacheKey("cacheable.AClass.insideClass(1)") {
      new AClass().insideClass(1)
    }
  }

  it should "work for a method inside a trait" in {
    checkCacheKey("cacheable.ATrait.insideTrait(1)") {
      new ATrait { val cacheConfig = CacheKeySpec.this.cacheConfig }.insideTrait(1)
    }
  }

  it should "work for a method inside an object" in {
    AnObject.cacheConfig = this.cacheConfig
    checkCacheKey("cacheable.AnObject.insideObject(1)") {
      AnObject.insideObject(1)
    }
  }

  it should "work for a method inside a class inside a class" in {
    checkCacheKey("cacheable.AClass.InnerClass.insideInnerClass(1)") {
      new AClass().inner.insideInnerClass(1)
    }
  }

  it should "work for a method inside an object inside a class" in {
    checkCacheKey("cacheable.AClass.InnerObject.insideInnerObject(1)") {
      new AClass().InnerObject.insideInnerObject(1)
    }
  }

  it should "work for a method inside a package object" in {
    pkg.cacheConfig = this.cacheConfig
    checkCacheKey("cacheable.pkg.package.insidePackageObject(1)") {
      pkg.insidePackageObject(1)
    }
  }

  def checkCacheKey(expectedKey: String)(call: => Int) {
    // Run the cacheable block, putting some value into the cache
    val value = call

    // Check that the value is in the cache, with the expected key
    cache.get(expectedKey) should be(Some(value))
  }

  def multipleArgLists(a: Int, b: String)(c: String, d: Int): Int = cacheable {
      123
  }

  case class CaseClass(a: Int) { override def toString = "custom toString" }
  def takesCaseClass(cc: CaseClass): Int = cacheable {
      123
  }

  def lazyArg(a: => Int): Int = cacheable {
    123
  }

  def functionArg(a: String => Int): Int = cacheable {
    123
  }

}

class AClass(implicit val cacheConfig: CacheConfig) {
  def insideClass(a: Int): Int = cacheable {
    123
  }

  class InnerClass {
    def insideInnerClass(a: Int): Int = cacheable {
      123
    }
  }
  val inner = new InnerClass

  object InnerObject {
    def insideInnerObject(a: Int): Int = cacheable {
      123
    }
  }
}

trait ATrait {
  implicit val cacheConfig: CacheConfig

  def insideTrait(a: Int): Int = cacheable {
    123
  }
}

object AnObject {
  implicit var cacheConfig: CacheConfig = null
  def insideObject(a: Int): Int = cacheable {
    123
  }
}