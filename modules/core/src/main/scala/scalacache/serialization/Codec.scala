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

import scala.annotation.implicitNotFound
import scala.util.{Failure, Success, Try}

trait Encoder[L, R] {
  def encode(left: L): R
}

trait Decoder[L, R] {
  def decode(right: R): Codec.DecodingResult[L]
}

/** Represents a type class that needs to be implemented for serialization/deserialization to work.
  */
@implicitNotFound(msg = """Could not find any Codecs for types ${L, R}.
If you would like to serialize values in a binary format, please import the binary codec:

import scalacache.serialization.binary._

If you would like to serialize values as JSON using circe, please import the circe codec
and provide a circe Encoder[${L}] and Decoder[${L}], e.g.:

import scalacache.serialization.circe._
import io.circe.generic.auto._

You will need a dependency on the scalacache-circe module.

See the documentation for more details on codecs.""")
trait Codec[L, R] extends Encoder[L, R] with Decoder[L, R] {
  override def encode(left: L): R
  override def decode(right: R): Codec.DecodingResult[L]
}

/** For simple primitives, we provide lightweight Codecs for ease of use.
  */
object Codec {

  type DecodingResult[T] = Either[FailedToDecode, T]

  def tryDecode[T](f: => T): DecodingResult[T] =
    Try(f) match {
      case Success(a) => Right(a)
      case Failure(e) => Left(FailedToDecode(e))
    }

}
