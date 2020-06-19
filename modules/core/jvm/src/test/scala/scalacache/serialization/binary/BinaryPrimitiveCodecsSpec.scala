package scalacache.serialization.binary

import org.scalacheck._
import org.scalatest.prop.GeneratorDrivenPropertyChecks

import scalacache.serialization.Codec
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Inspired by Shade @ https://github.com/alexandru/shade/blob/master/src/test/scala/shade/tests/CodecsSuite.scala
  */
class BinaryPrimitiveCodecsSpec extends AnyFlatSpec with Matchers with GeneratorDrivenPropertyChecks {

  private def serdesCheck[A: Arbitrary](implicit codec: Codec[A]): Unit = {
    forAll { n: A =>
      val serialised   = codec.encode(n)
      val deserialised = codec.decode(serialised)
      deserialised shouldBe Right(n)
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
