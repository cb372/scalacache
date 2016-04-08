package scalacache.serialization

import scala.annotation.implicitNotFound
import scala.language.implicitConversions

/**
 * Represents a type class that needs to be implemented
 * for serialization/deserialization to work.
 */
@implicitNotFound("Could not find any Codecs for ${From} -> ${To}. Please provide one or import scalacache._")
trait Codec[From, To] {
  def serialize(value: From): To
  def deserialize(data: To): From
}

/**
 * For simple primitives, we provide lightweight Codecs for ease of use.
 */
object Codec extends BaseCodecs