package scalacache.serialization

import java.io._

import scala.reflect.ClassTag
import scala.util.control.NonFatal
import scalacache.serialization.Codec.DecodingResult

/**
  * Holds a Java-serialisation-based Codec[Object <: Serializable] instance
  *
  * Credit: Shade @ https://github.com/alexandru/shade/blob/master/src/main/scala/shade/memcached/Codec.scala
  */
trait JavaSerializationCodec {

  /**
    * Uses plain Java serialization to deserialize objects
    */
  implicit def AnyRefBinaryCodec[S <: Serializable](implicit ev: ClassTag[S]): Codec[S] =
    new JavaSerializationAnyCodec[S](ev)

}

class JavaSerializationAnyCodec[S <: Serializable](classTag: ClassTag[S]) extends Codec[S] {

  def using[T <: Closeable, R](obj: T)(f: T => R): R =
    try f(obj)
    finally try obj.close()
    catch {
      case NonFatal(_) => // does nothing
    }

  def encode(value: S): Array[Byte] =
    using(new ByteArrayOutputStream()) { buf =>
      using(new ObjectOutputStream(buf)) { out =>
        out.writeObject(value)
        out.close()
        buf.toByteArray
      }
    }

  def decode(data: Array[Byte]): DecodingResult[S] =
    Codec.tryDecode {
      using(new ByteArrayInputStream(data)) { buf =>
        val in = new GenericCodecObjectInputStream(classTag, buf)
        using(in) { inp =>
          inp.readObject().asInstanceOf[S]
        }
      }
    }
}
