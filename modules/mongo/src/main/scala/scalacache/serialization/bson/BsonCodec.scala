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

package scalacache.serialization.bson

import org.bson.BsonValue
import scalacache.serialization.{Codec, Decoder, Encoder}
import org.bson.conversions.Bson

trait BsonEncoder[T] extends Encoder[T, BsonValue] {
  override def encode(value: T): BsonValue
}

trait BsonDecoder[T] extends Decoder[T, BsonValue] {
  override def decode(value: BsonValue): Codec.DecodingResult[T]
}

trait BsonCodec[T] extends Codec[T, BsonValue] with BsonEncoder[T] with BsonDecoder[T] {
  override def encode(value: T): BsonValue
  override def decode(bytes: BsonValue): Codec.DecodingResult[T]
}
