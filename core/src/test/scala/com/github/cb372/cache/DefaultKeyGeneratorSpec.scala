package com.github.cb372.cache

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

/**
 * Author: chris
 * Created: 2/19/13
 */
class DefaultKeyGeneratorSpec extends FlatSpec with ShouldMatchers {

  val keyGen = KeyGenerator.defaultGenerator

  behavior of "KeyGenerator.defaultGenerator"

  it should "build a key for a no-arg method with no class" in {
    keyGen.toCacheKey("", "myMethod", Nil) should be ("myMethod")
  }

  it should "build a key for a no-arg method" in {
    keyGen.toCacheKey("MyClass", "myMethod", Nil) should be ("MyClass.myMethod")
  }

  it should "build a key for a one-arg method" in {
    keyGen.toCacheKey("MyClass", "myMethod", List(List("foo"))) should be ("MyClass.myMethod(foo)")
  }

  it should "build a key for a two-arg method" in {
    keyGen.toCacheKey("MyClass", "myMethod", List(List("foo", 123))) should be ("MyClass.myMethod(foo, 123)")
  }

  it should "build a key for a method with multiple argument lists" in {
    keyGen.toCacheKey("MyClass", "myMethod", List(List("foo", 123), List(3.0))) should be ("MyClass.myMethod(foo, 123)(3.0)")
  }

}
