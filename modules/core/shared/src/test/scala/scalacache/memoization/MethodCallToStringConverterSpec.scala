package scalacache.memoization

import org.scalatest.FlatSpec
import org.scalatest.Matchers

import scalacache.memoization.MethodCallToStringConverter._

class MethodCallToStringConverterSpec extends FlatSpec with Matchers {

  behavior of "excludeClassConstructorParams"

  it should "build a key for a no-arg method with no class" in {
    excludeClassConstructorParams.toString("", Vector.empty, "myMethod", Vector.empty) should be("myMethod")
  }

  it should "build a key for a no-arg method" in {
    excludeClassConstructorParams.toString("MyClass", Vector.empty, "myMethod", Vector.empty) should be(
      "MyClass.myMethod")
  }

  it should "build a key for a one-arg method" in {
    excludeClassConstructorParams.toString("MyClass", Vector.empty, "myMethod", Vector(Vector("foo"))) should be(
      "MyClass.myMethod(foo)")
  }

  it should "build a key for a two-arg method" in {
    excludeClassConstructorParams.toString("MyClass", Vector.empty, "myMethod", Vector(Vector("foo", 123))) should be(
      "MyClass.myMethod(foo, 123)")
  }

  it should "build a key for a method with multiple argument lists" in {
    excludeClassConstructorParams.toString("MyClass", Vector.empty, "myMethod", Vector(Vector("foo", 123), Vector(3.4))) should be(
      "MyClass.myMethod(foo, 123)(3.4)")
  }

  it should "ignore class constructor arguments" in {
    excludeClassConstructorParams.toString("MyClass", Vector(Vector("foo", "bar")), "myMethod", Vector.empty) should be(
      "MyClass.myMethod")
  }

  behavior of "includeClassConstructorParams"

  it should "build a key for a method with multiple argument lists" in {
    includeClassConstructorParams.toString("MyClass",
                                           Vector(Vector("foo", "bar"), Vector("baz")),
                                           "myMethod",
                                           Vector(Vector("foo", 123), Vector(3.4))) should be(
      "MyClass(foo, bar)(baz).myMethod(foo, 123)(3.4)")
  }

  it should "build a key for a method in an object" in {
    includeClassConstructorParams.toString("MyObject", Vector.empty, "myMethod", Vector.empty) should be(
      "MyObject.myMethod")
  }

}
