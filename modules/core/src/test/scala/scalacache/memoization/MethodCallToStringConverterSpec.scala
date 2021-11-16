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

import scalacache.memoization.MethodCallToStringConverter._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MethodCallToStringConverterSpec extends AnyFlatSpec with Matchers {

  behavior of "excludeClassConstructorParams"

  it should "build a key for a no-arg method with no class" in {
    excludeClassConstructorParams.toString("", Vector.empty, "myMethod", Vector.empty) should be("myMethod")
  }

  it should "build a key for a no-arg method" in {
    excludeClassConstructorParams.toString("MyClass", Vector.empty, "myMethod", Vector.empty) should be(
      "MyClass.myMethod"
    )
  }

  it should "build a key for a one-arg method" in {
    excludeClassConstructorParams.toString("MyClass", Vector.empty, "myMethod", Vector(Vector("foo"))) should be(
      "MyClass.myMethod(foo)"
    )
  }

  it should "build a key for a two-arg method" in {
    excludeClassConstructorParams.toString("MyClass", Vector.empty, "myMethod", Vector(Vector("foo", 123))) should be(
      "MyClass.myMethod(foo, 123)"
    )
  }

  it should "build a key for a method with multiple argument lists" in {
    excludeClassConstructorParams.toString(
      "MyClass",
      Vector.empty,
      "myMethod",
      Vector(Vector("foo", 123), Vector(3.4))
    ) should be(
      "MyClass.myMethod(foo, 123)(3.4)"
    )
  }

  it should "ignore class constructor arguments" in {
    excludeClassConstructorParams.toString("MyClass", Vector(Vector("foo", "bar")), "myMethod", Vector.empty) should be(
      "MyClass.myMethod"
    )
  }

  behavior of "includeClassConstructorParams"

  it should "build a key for a method with multiple argument lists" in {
    includeClassConstructorParams.toString(
      "MyClass",
      Vector(Vector("foo", "bar"), Vector("baz")),
      "myMethod",
      Vector(Vector("foo", 123), Vector(3.4))
    ) should be("MyClass(foo, bar)(baz).myMethod(foo, 123)(3.4)")
  }

  it should "build a key for a method in an object" in {
    includeClassConstructorParams.toString("MyObject", Vector.empty, "myMethod", Vector.empty) should be(
      "MyObject.myMethod"
    )
  }

}
