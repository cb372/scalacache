package scalacache.serialization

import scala.annotation.implicitNotFound
import scala.language.implicitConversions

/**
 * Represents a type class that needs to be implemented
 * for serialization/deserialization to work.
 */
@implicitNotFound("Could not find any Codecs for type ${From} and ${Repr}. Please provide one or import scalacache._")
trait Codec[From, Repr] {
  def serialize(value: From): Repr
  def deserialize(data: Repr): From
}

/**
 * For simple primitives, we provide lightweight Codecs for ease of use.
 */
object Codec extends BaseCodecs