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

import org.scalatest._

import scalacache._
import cats.effect.SyncIO
import org.scalatest.matchers.should.Matchers
import scala.annotation.nowarn

trait CacheKeySpecCommon extends Suite with Matchers with BeforeAndAfter {

  implicit lazy val config: MemoizationConfig = scalacache.memoization.MemoizationConfig.defaultMemoizationConfig

  implicit lazy val cache: MockCache[SyncIO, Int] = new MockCache()

  before {
    cache.mmap.clear()
  }

  def checkCacheKey(expectedKey: String)(call: => Int): Assertion = {
    // Run the memoize block, putting some value into the cache
    val value = call

    // Check that the value is in the cache, with the expected key
    cache.get(expectedKey).unsafeRunSync() should be(Some(value))
  }

  @nowarn
  def multipleArgLists(a: Int, b: String)(c: String, d: Int): Int =
    memoize(None) {
      123
    }.unsafeRunSync()

  case class CaseClass(a: Int) { override def toString = "custom toString" }

  @nowarn
  def takesCaseClass(cc: CaseClass): SyncIO[Int] = memoize(None) {
    123
  }

  @nowarn
  def lazyArg(a: => Int): SyncIO[Int] = memoize(None) {
    123
  }

  @nowarn
  def functionArg(a: String => Int): SyncIO[Int] = memoize(None) {
    123
  }

  @nowarn
  def withExcludedParams(a: Int, @cacheKeyExclude b: String, c: String)(@cacheKeyExclude d: Int): SyncIO[Int] =
    memoize(None) {
      123
    }

}

class AClass[F[_]]()(implicit cache: Cache[F, String, Int], config: MemoizationConfig) {
  @nowarn
  def insideClass(a: Int): F[Int] = memoize(None) {
    123
  }

  class InnerClass {
    @nowarn
    def insideInnerClass(a: Int): F[Int] = memoize(None) {
      123
    }
  }
  val inner = new InnerClass

  object InnerObject {
    @nowarn
    def insideInnerObject(a: Int): F[Int] = memoize(None) {
      123
    }
  }
}

trait ATrait[F[_]] {
  implicit val cache: Cache[F, String, Int]
  implicit val config: MemoizationConfig

  @nowarn
  def insideTrait(a: Int): F[Int] = memoize(None) {
    123
  }
}

object AnObject {
  implicit var cache: Cache[SyncIO, String, Int] = null
  implicit var config: MemoizationConfig         = null
  @nowarn
  def insideObject(a: Int): SyncIO[Int] = memoize(None) {
    123
  }
}

class ClassWithConstructorParams[F[_]](b: Int) {
  implicit var cache: Cache[F, String, Int] = null
  implicit var config: MemoizationConfig    = null
  def foo(a: Int): F[Int] = memoize(None) {
    a + b
  }
}

class ClassWithExcludedConstructorParam[F[_]](b: Int, @cacheKeyExclude c: Int) {
  implicit var cache: Cache[F, String, Int] = null
  implicit var config: MemoizationConfig    = null
  def foo(a: Int): F[Int] = memoize(None) {
    a + b + c
  }
}
