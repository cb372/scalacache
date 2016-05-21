package scalacache.memoization

import org.scalatest._

import scalacache._
import scalacache.memoization.MethodCallToStringConverter.excludeClassConstructorParams
import scalacache.serialization.InMemoryRepr

class CacheKeyExcludingConstructorParamsSpec extends FlatSpec with CacheKeySpecCommon {

  behavior of "cache key generation for method memoization (not including constructor params in cache key)"

  implicit val scalaCache: ScalaCache[InMemoryRepr] = ScalaCache(cache, memoization = MemoizationConfig(toStringConverter = excludeClassConstructorParams))

  it should "not include the enclosing class's constructor params in the cache key" in {
    val instance1 = new ClassWithConstructorParams(50)
    instance1.scalaCache = scalaCache

    val instance2 = new ClassWithConstructorParams(100)
    instance2.scalaCache = scalaCache

    checkCacheKey("scalacache.memoization.ClassWithConstructorParams.foo(42)") {
      instance1.foo(42)
    }

    checkCacheKey("scalacache.memoization.ClassWithConstructorParams.foo(42)") {
      instance2.foo(42)
    }
  }

  it should "include values of all arguments for all argument lists" in {
    checkCacheKey("scalacache.memoization.CacheKeySpecCommon.multipleArgLists(1, 2)(3, 4)") {
      multipleArgLists(1, "2")("3", 4)
    }
  }

  it should "call toString on arguments to convert them into a string" in {
    checkCacheKey("scalacache.memoization.CacheKeySpecCommon.takesCaseClass(custom toString)") {
      takesCaseClass(CaseClass(1))
    }
  }

  it should "include values of lazy arguments" in {
    checkCacheKey("scalacache.memoization.CacheKeySpecCommon.lazyArg(1)") {
      lazyArg(1)
    }
  }

  it should "exclude values of arguments annotated with @cacheKeyExclude" in {
    checkCacheKey("scalacache.memoization.CacheKeySpecCommon.withExcludedParams(1, 3)()") {
      withExcludedParams(1, "2", "3")(4)
    }
  }

  it should "work for a method inside a class" in {
    checkCacheKey("scalacache.memoization.AClass.insideClass(1)") {
      new AClass().insideClass(1)
    }
  }

  it should "work for a method inside a trait" in {
    checkCacheKey("scalacache.memoization.ATrait.insideTrait(1)") {
      new ATrait { val scalaCache = CacheKeyExcludingConstructorParamsSpec.this.scalaCache }.insideTrait(1)
    }
  }

  it should "work for a method inside an object" in {
    AnObject.scalaCache = this.scalaCache
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
    pkg.scalaCache = this.scalaCache
    checkCacheKey("scalacache.memoization.pkg.package.insidePackageObject(1)") {
      pkg.insidePackageObject(1)
    }
  }

}
