package scalacache.serialization

import org.scalacheck._
import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.prop.GeneratorDrivenPropertyChecks

/**
  * Inspired by Shade @ https://github.com/alexandru/shade/blob/master/src/test/scala/shade/tests/CodecsSuite.scala
  */
class BasicCodecsSpec
    extends FlatSpec
    with Matchers
    with GeneratorDrivenPropertyChecks {

  private def serdesCheck[A: Arbitrary](
      implicit codec: Codec[A, Array[Byte]]): Unit = {
    forAll { n: A =>
      val serialised = codec.serialize(n)
      val deserialised = codec.deserialize(serialised)
      deserialised shouldBe n
    }
  }

  it should "serialize and deserialize Ints" in {
    serdesCheck[Int]
  }

  it should "serialize and deserialize Longs" in {
    serdesCheck[Long]
  }

  it should "serialize and deserialize Doubles" in {
    serdesCheck[Double]
  }

  it should "serialize and deserialize Floats" in {
    serdesCheck[Float]
  }

  it should "serialize and deserialize Booleans" in {
    serdesCheck[Boolean]
  }

  it should "serialize and deserialize Char" in {
    serdesCheck[Char]
  }

  it should "serialize and deserialize Short" in {
    serdesCheck[Short]
  }

  it should "serialize and deserialize String" in {
    serdesCheck[String]
  }

  it should "serialize and deserialize Array[Byte]" in {
    serdesCheck[Array[Byte]]
  }

}
