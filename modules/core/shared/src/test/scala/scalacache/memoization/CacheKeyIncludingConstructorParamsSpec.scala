package scalacache.memoization

import org.scalatest._
import scalacache._
import scalacache.memoization.MethodCallToStringConverter._

class CacheKeyIncludingConstructorParamsSpec extends FlatSpec with CacheKeySpecCommon { self =>

  behavior of "cache key generation for method memoization (when including constructor params in cache key)"

  implicit val config: CacheConfig =
    CacheConfig(memoization = MemoizationConfig(toStringConverter = includeClassConstructorParams))

  it should "include the enclosing class's constructor params in the cache key" in {
    val instance = new ClassWithConstructorParams(50)
    instance.cache = cache

    checkCacheKey("scalacache.memoization.ClassWithConstructorParams(50).foo(42)") {
      instance.foo(42)
    }
  }

  it should "exclude values of constructor params annotated with @cacheKeyExclude" in {
    val instance = new ClassWithExcludedConstructorParam(50, 10)
    instance.cache = cache

    checkCacheKey("scalacache.memoization.ClassWithExcludedConstructorParam(50).foo(42)") {
      instance.foo(42)
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
    // The class's implicit param (the Cache) should be included in the cache key)
    checkCacheKey(s"scalacache.memoization.AClass()(${cache.toString}).insideClass(1)") {
      new AClass().insideClass(1)
    }
  }

  it should "work for a method inside a trait" in {
    checkCacheKey("scalacache.memoization.ATrait.insideTrait(1)") {
      new ATrait { val cache = self.cache }.insideTrait(1)
    }
  }

  it should "work for a method inside an object" in {
    AnObject.cache = this.cache
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
    pkg.cache = this.cache
    checkCacheKey("scalacache.memoization.pkg.package.insidePackageObject(1)") {
      pkg.insidePackageObject(1)
    }
  }
}
