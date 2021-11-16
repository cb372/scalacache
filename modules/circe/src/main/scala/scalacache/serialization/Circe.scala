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

import java.nio.ByteBuffer
import io.circe.jawn.JawnParser
import scalacache.serialization.binary.BinaryCodec

package object circe {

  private[this] val parser = new JawnParser

  implicit def codec[A](implicit encoder: io.circe.Encoder[A], decoder: io.circe.Decoder[A]): BinaryCodec[A] =
    new BinaryCodec[A] {

      override def encode(value: A): Array[Byte] = encoder.apply(value).noSpaces.getBytes("utf-8")

      override def decode(bytes: Array[Byte]): Codec.DecodingResult[A] =
        parser.decodeByteBuffer(ByteBuffer.wrap(bytes)).left.map(FailedToDecode.apply)

    }

}
