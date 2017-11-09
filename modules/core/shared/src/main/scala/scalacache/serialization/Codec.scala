package scalacache.serialization

import scala.annotation.implicitNotFound
import scala.language.implicitConversions
import scala.util.Try

/**
  * Represents a type class that needs to be implemented
  * for serialization/deserialization to work.
  */
@implicitNotFound("Could not find any Codecs for type ${A}. Please provide one or import scalacache._")
trait Codec[A] {
  def encode(value: A): Array[Byte]
  def decode(bytes: Array[Byte]): Codec.DecodingResult[A]
}

/**
  * For simple primitives, we provide lightweight Codecs for ease of use.
  */
object Codec extends BaseCodecs {

  type DecodingResult[A] = Either[FailedToDecode, A]

  def tryDecode[A](f: => A): DecodingResult[A] =
    Try(f).toEither.left.map(FailedToDecode)

}
