package scalacache.serdes

import scala.annotation.implicitNotFound
import scala.language.implicitConversions

/**
 * Represents a type class that needs to be implemented
 * for serialization/deserialization to work.
 */
@implicitNotFound("Could not find any Codecs for type ${T}. Please provide one or import scalacache.serdes.JavaSerializationCodecs._")
trait Codec[T] {
  def serialize(value: T): Array[Byte]
  def deserialize(data: Array[Byte]): T
}

/**
 * For simple primitives, we provide lightweight Codecs for ease of use.
 */
object Codec extends BaseCodecs