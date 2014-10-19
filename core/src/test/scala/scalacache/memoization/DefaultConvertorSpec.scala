package scalacache.memoization

import org.scalatest.FlatSpec
import org.scalatest.Matchers

class DefaultConvertorSpec extends FlatSpec with Matchers {

  val convertor = MethodCallToStringConvertor.defaultConvertor

  behavior of "KeyGenerator.defaultGenerator"

  it should "build a key for a no-arg method with no class" in {
    convertor.toString("", "myMethod", Nil) should be("myMethod")
  }

  it should "build a key for a no-arg method" in {
    convertor.toString("MyClass", "myMethod", Nil) should be("MyClass.myMethod")
  }

  it should "build a key for a one-arg method" in {
    convertor.toString("MyClass", "myMethod", List(List("foo"))) should be("MyClass.myMethod(foo)")
  }

  it should "build a key for a two-arg method" in {
    convertor.toString("MyClass", "myMethod", List(List("foo", 123))) should be("MyClass.myMethod(foo, 123)")
  }

  it should "build a key for a method with multiple argument lists" in {
    convertor.toString("MyClass", "myMethod", List(List("foo", 123), List(3.0))) should be("MyClass.myMethod(foo, 123)(3.0)")
  }

}
