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

import scalacache.serialization.{Codec, Decoder, Encoder}

trait BinaryEncoder[T] extends Encoder[T, Array[Byte]] {
  override def encode(value: T): Array[Byte]
}

trait BinaryDecoder[T] extends Decoder[T, Array[Byte]] {
  override def decode(value: Array[Byte]): Codec.DecodingResult[T]
}

trait BinaryCodec[T] extends Codec[T, Array[Byte]] with BinaryEncoder[T] with BinaryDecoder[T] {
  override def encode(value: T): Array[Byte]
  override def decode(bytes: Array[Byte]): Codec.DecodingResult[T]
}
