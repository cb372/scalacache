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

package scalacache.serialization.binary

import scalacache.serialization.Codec._

/** Codecs for all the Java primitive types, plus String and Array[Byte]
  *
  * Credit: Shade @ https://github.com/alexandru/shade/blob/master/src/main/scala/shade/memcached/Codec.scala
  */
trait BinaryPrimitiveCodecs {

  implicit object IntBinaryCodec extends BinaryCodec[Int] {
    def encode(value: Int): Array[Byte] =
      Array(
        (value >>> 24).asInstanceOf[Byte],
        (value >>> 16).asInstanceOf[Byte],
        (value >>> 8).asInstanceOf[Byte],
        value.asInstanceOf[Byte]
      )

    def decode(data: Array[Byte]): DecodingResult[Int] = tryDecode(
      (data(0).asInstanceOf[Int] & 255) << 24 |
        (data(1).asInstanceOf[Int] & 255) << 16 |
        (data(2).asInstanceOf[Int] & 255) << 8 |
        data(3).asInstanceOf[Int] & 255
    )
  }

  implicit object DoubleBinaryCodec extends BinaryCodec[Double] {
    import java.lang.{Double => JvmDouble}
    def encode(value: Double): Array[Byte] = {
      val l = JvmDouble.doubleToLongBits(value)
      LongBinaryCodec.encode(l)
    }

    def decode(data: Array[Byte]): DecodingResult[Double] = {
      LongBinaryCodec
        .decode(data)
        .map(l => JvmDouble.longBitsToDouble(l))
    }
  }

  implicit object FloatBinaryCodec extends BinaryCodec[Float] {
    import java.lang.{Float => JvmFloat}
    def encode(value: Float): Array[Byte] = {
      val i = JvmFloat.floatToIntBits(value)
      IntBinaryCodec.encode(i)
    }

    def decode(data: Array[Byte]): DecodingResult[Float] = {
      IntBinaryCodec
        .decode(data)
        .map(i => JvmFloat.intBitsToFloat(i))
    }
  }

  implicit object LongBinaryCodec extends BinaryCodec[Long] {
    def encode(value: Long): Array[Byte] =
      Array(
        (value >>> 56).asInstanceOf[Byte],
        (value >>> 48).asInstanceOf[Byte],
        (value >>> 40).asInstanceOf[Byte],
        (value >>> 32).asInstanceOf[Byte],
        (value >>> 24).asInstanceOf[Byte],
        (value >>> 16).asInstanceOf[Byte],
        (value >>> 8).asInstanceOf[Byte],
        value.asInstanceOf[Byte]
      )

    def decode(data: Array[Byte]): DecodingResult[Long] = tryDecode(
      (data(0).asInstanceOf[Long] & 255) << 56 |
        (data(1).asInstanceOf[Long] & 255) << 48 |
        (data(2).asInstanceOf[Long] & 255) << 40 |
        (data(3).asInstanceOf[Long] & 255) << 32 |
        (data(4).asInstanceOf[Long] & 255) << 24 |
        (data(5).asInstanceOf[Long] & 255) << 16 |
        (data(6).asInstanceOf[Long] & 255) << 8 |
        data(7).asInstanceOf[Long] & 255
    )
  }

  implicit object BooleanBinaryCodec extends BinaryCodec[Boolean] {
    def encode(value: Boolean): Array[Byte] =
      Array((if (value) 1 else 0).asInstanceOf[Byte])

    def decode(data: Array[Byte]): DecodingResult[Boolean] =
      tryDecode(data.isDefinedAt(0) && data(0) == 1)
  }

  implicit object CharBinaryCodec extends BinaryCodec[Char] {
    def encode(value: Char): Array[Byte] = Array(
      (value >>> 8).asInstanceOf[Byte],
      value.asInstanceOf[Byte]
    )

    def decode(data: Array[Byte]): DecodingResult[Char] = tryDecode(
      ((data(0).asInstanceOf[Int] & 255) << 8 |
        data(1).asInstanceOf[Int] & 255)
        .asInstanceOf[Char]
    )
  }

  implicit object ShortBinaryCodec extends BinaryCodec[Short] {
    def encode(value: Short): Array[Byte] = Array(
      (value >>> 8).asInstanceOf[Byte],
      value.asInstanceOf[Byte]
    )

    def decode(data: Array[Byte]): DecodingResult[Short] = tryDecode(
      ((data(0).asInstanceOf[Short] & 255) << 8 |
        data(1).asInstanceOf[Short] & 255)
        .asInstanceOf[Short]
    )
  }

  implicit object StringBinaryCodec extends BinaryCodec[String] {
    def encode(value: String): Array[Byte]                = value.getBytes("UTF-8")
    def decode(data: Array[Byte]): DecodingResult[String] = tryDecode(new String(data, "UTF-8"))
  }

  implicit object ArrayByteBinaryCodec extends BinaryCodec[Array[Byte]] {
    def encode(value: Array[Byte]): Array[Byte]                = value
    def decode(data: Array[Byte]): DecodingResult[Array[Byte]] = Right(data)
  }
}
