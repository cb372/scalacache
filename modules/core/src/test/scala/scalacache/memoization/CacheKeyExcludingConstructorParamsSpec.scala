/*
 * Copyright 2021 scalacache
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package scalacache.memoization

import cats.effect.SyncIO
import org.scalatest.flatspec.AnyFlatSpec
import scalacache.memoization.MethodCallToStringConverter.excludeClassConstructorParams

class CacheKeyExcludingConstructorParamsSpec extends AnyFlatSpec with CacheKeySpecCommon { self =>

  behavior of "cache key generation for method memoization (not including constructor params in cache key)"

  override implicit lazy val config: MemoizationConfig =
    MemoizationConfig(toStringConverter = excludeClassConstructorParams)

  it should "not include the enclosing class's constructor params in the cache key" in {
    val instance1 = new ClassWithConstructorParams[SyncIO](50)
    instance1.cache = cache
    instance1.config = config

    val instance2 = new ClassWithConstructorParams[SyncIO](100)
    instance2.cache = cache
    instance2.config = config

    checkCacheKey("scalacache.memoization.ClassWithConstructorParams.foo(42)") {
      instance1.foo(42).unsafeRunSync()
    }

    checkCacheKey("scalacache.memoization.ClassWithConstructorParams.foo(42)") {
      instance2.foo(42).unsafeRunSync()
    }
  }

  it should "include values of all arguments for all argument lists" in {
    checkCacheKey("scalacache.memoization.CacheKeySpecCommon.multipleArgLists(1, 2)(3, 4)") {
      multipleArgLists(1, "2")("3", 4)
    }
  }

  it should "call toString on arguments to convert them into a string" in {
    checkCacheKey("scalacache.memoization.CacheKeySpecCommon.takesCaseClass(custom toString)") {
      takesCaseClass(CaseClass(1)).unsafeRunSync()
    }
  }

  it should "include values of lazy arguments" in {
    checkCacheKey("scalacache.memoization.CacheKeySpecCommon.lazyArg(1)") {
      lazyArg(1).unsafeRunSync()
    }
  }

  it should "exclude values of arguments annotated with @cacheKeyExclude" in {
    checkCacheKey("scalacache.memoization.CacheKeySpecCommon.withExcludedParams(1, 3)()") {
      withExcludedParams(1, "2", "3")(4).unsafeRunSync()
    }
  }

  it should "work for a method inside a class" in {
    checkCacheKey("scalacache.memoization.AClass.insideClass(1)") {
      new AClass[SyncIO]().insideClass(1).unsafeRunSync()
    }
  }

  it should "work for a method inside a trait" in {
    checkCacheKey("scalacache.memoization.ATrait.insideTrait(1)") {
      new ATrait[SyncIO] { val cache = self.cache; val config = self.config }.insideTrait(1).unsafeRunSync()
    }
  }

  it should "work for a method inside an object" in {
    AnObject.cache = this.cache
    checkCacheKey("scalacache.memoization.AnObject.insideObject(1)") {
      AnObject.insideObject(1).unsafeRunSync()
    }
  }

  it should "work for a method inside a class inside a class" in {
    checkCacheKey("scalacache.memoization.AClass.InnerClass.insideInnerClass(1)") {
      new AClass[SyncIO]().inner.insideInnerClass(1).unsafeRunSync()
    }
  }

  it should "work for a method inside an object inside a class" in {
    checkCacheKey("scalacache.memoization.AClass.InnerObject.insideInnerObject(1)") {
      new AClass[SyncIO]().InnerObject.insideInnerObject(1).unsafeRunSync()
    }
  }

  it should "work for a method inside a package object" in {
    pkg.cache = this.cache
    checkCacheKey("scalacache.memoization.pkg.package.insidePackageObject(1)") {
      pkg.insidePackageObject(1).unsafeRunSync()
    }
  }

}
