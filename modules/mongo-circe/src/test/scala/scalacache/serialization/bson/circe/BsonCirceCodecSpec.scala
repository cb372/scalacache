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

package scalacache.serialization

import org.scalacheck.Arbitrary
import org.scalatest.compatible.Assertion
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scalacache.serialization.bson.BsonCodec
import scalacache.serialization.bson.BsonEncoder

sealed abstract class Item                                            extends Product with Serializable
case class Fruit(name: String, tastinessQuotient: Option[BigDecimal]) extends Item
case class Veg(name: String, tastinessQuotient: Option[BigDecimal])   extends Item
case class Purchase(item: Item, quantity: Int)
case class Basket(contents: Set[Purchase])

sealed abstract class FolderEntry                           extends Product with Serializable
case class Folder(name: String, contents: Set[FolderEntry]) extends FolderEntry
case class File(name: String)                               extends FolderEntry

case class Wrapper(string: String) extends AnyVal

class BsonCirceCodecSpec extends AnyFlatSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  behavior of "BSON serialization using circe"

  import scalacache.serialization.bson.circe._

  private def serdesCheck[A: Arbitrary](implicit codec: BsonCodec[A]): Assertion = {
    forAll(minSuccessful(10000)) { (a: A) =>
      val deserialised = codec.decode(codec.encode(a))
      deserialised.toOption.get shouldBe a
    }
  }

  it should "produce BSON encoders from Circe encoders" in {
    import io.circe.generic.semiauto._

    implicit val encoder: io.circe.Encoder[Wrapper] = deriveEncoder[Wrapper]

    implicitly[BsonEncoder[Wrapper]]
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

  it should "serialize and deserialize BigDecimals" in {
    serdesCheck[BigDecimal]
  }

  it should "serialize and deserialize BigInts" in {
    serdesCheck[BigInt]
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

  it should "serialize and deserialize a case class" in {
    import io.circe.generic.auto._
    val fruitCodec   = implicitly[BsonCodec[Fruit]]
    val banana       = Fruit("banana", Some(BigDecimal(0.7)))
    val deserialised = fruitCodec.decode(fruitCodec.encode(banana))
    deserialised.toOption.get shouldBe banana
  }

  it should "serialize and deserialize a nested case class" in {
    import io.circe.generic.auto._

    val basketCodec = implicitly[BsonCodec[Basket]]

    val basket = Basket(
      Set(
        Purchase(Fruit("banana", Some(BigDecimal(0.7))), 1),
        Purchase(Veg("carrot", Some(BigDecimal(0.7))), 2),
        Purchase(Fruit("mango", Some(BigDecimal("1" * 100))), 1),
        Purchase(Fruit("strawberry", None), 10)
      )
    )

    val deserialised = basketCodec.decode(basketCodec.encode(basket))
    deserialised.toOption.get shouldBe basket
  }

  it should "serialize and deserialize a recursive case class" in {
    import io.circe.generic.semiauto._

    // Circe auto derivation of recursive case classes does not work on Scala 3.x
    implicit lazy val folderEntryCirceCodec: io.circe.Codec[FolderEntry] =
      deriveCodec[FolderEntry]

    val folderEntryCodec: BsonCodec[FolderEntry] = implicitly[BsonCodec[FolderEntry]]

    val folder = Folder("folder", Set(File("foo.txt"), Folder("subfolder", Set(File("bar.txt"))), File("baz.txt")))

    val deserialised = folderEntryCodec.decode(folderEntryCodec.encode(folder))
    deserialised.toOption.get shouldBe folder
  }
}
