package scalacache.memoization

import org.scalatest.FlatSpec
import org.scalatest.Matchers

import scalacache.memoization.MethodCallToStringConverter._

class MethodCallToStringConverterSpec extends FlatSpec with Matchers {

  behavior of "excludeClassConstructorParams"

  it should "build a key for a no-arg method with no class" in {
    excludeClassConstructorParams.toString("", Nil, "myMethod", Nil) should be("myMethod")
  }

  it should "build a key for a no-arg method" in {
    excludeClassConstructorParams.toString("MyClass", Nil, "myMethod", Nil) should be("MyClass.myMethod")
  }

  it should "build a key for a one-arg method" in {
    excludeClassConstructorParams.toString("MyClass", Nil, "myMethod", List(List("foo"))) should be("MyClass.myMethod(foo)")
  }

  it should "build a key for a two-arg method" in {
    excludeClassConstructorParams.toString("MyClass", Nil, "myMethod", List(List("foo", 123))) should be("MyClass.myMethod(foo, 123)")
  }

  it should "build a key for a method with multiple argument lists" in {
    excludeClassConstructorParams.toString("MyClass", Nil, "myMethod", List(List("foo", 123), List(3.0))) should be("MyClass.myMethod(foo, 123)(3.0)")
  }

  it should "ignore class constructor arguments" in {
    excludeClassConstructorParams.toString("MyClass", List(List("foo", "bar")), "myMethod", Nil) should be("MyClass.myMethod")
  }

  behavior of "includeClassConstructorParams"

  it should "build a key for a method with multiple argument lists" in {
    includeClassConstructorParams.toString(
      "MyClass", List(List("foo", "bar"), List("baz")), "myMethod", List(List("foo", 123), List(3.0))) should be("MyClass(foo, bar)(baz).myMethod(foo, 123)(3.0)")
  }

  it should "build a key for a method in an object" in {
    includeClassConstructorParams.toString("MyObject", Nil, "myMethod", Nil) should be("MyObject.myMethod")
  }

}
