package scalacache.serialization

import scala.annotation.implicitNotFound
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
  * Represents a type class that needs to be implemented
  * for serialization/deserialization to work.
  */
@implicitNotFound(msg = """Could not find any Codecs for type ${A}.
If you would like to serialize values in a binary format, please import the binary codec:

import scalacache.serialization.binary._

If you would like to serialize values as JSON using circe, please import the circe codec
and provide a circe Encoder[${A}] and Decoder[${A}], e.g.:

import scalacache.serialization.circe._
import io.circe.generic.auto._

You will need a dependency on the scalacache-circe module.

See the documentation for more details on codecs.""")
trait Codec[A] {
  def encode(value: A): Array[Byte]
  def decode(bytes: Array[Byte]): Codec.DecodingResult[A]
}

/**
  * For simple primitives, we provide lightweight Codecs for ease of use.
  */
object Codec {

  type DecodingResult[A] = Either[FailedToDecode, A]

  def tryDecode[A](f: => A): DecodingResult[A] =
    Try(f) match {
      case Success(a) => Right(a)
      case Failure(e) => Left(FailedToDecode(e))
    }

}
