package scalacache.serialization

import scala.annotation.implicitNotFound
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

trait Encoder[L, R] {
  def encode(left: L): R
}

trait Decoder[L, R] {
  def decode(right: R): Codec.DecodingResult[L]
}

/**
  * Represents a type class that needs to be implemented
  * for serialization/deserialization to work.
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

/**
  * For simple primitives, we provide lightweight Codecs for ease of use.
  */
object Codec {

  type DecodingResult[T] = Either[FailedToDecode, T]

  def tryDecode[T](f: => T): DecodingResult[T] =
    Try(f) match {
      case Success(a) => Right(a)
      case Failure(e) => Left(FailedToDecode(e))
    }

}
