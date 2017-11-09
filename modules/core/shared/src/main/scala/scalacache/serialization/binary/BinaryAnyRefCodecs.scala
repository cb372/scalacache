package scalacache.serialization.binary

import scala.reflect.ClassTag
import scalacache.serialization.Codec

trait BinaryAnyRefCodecs_1 {

  /*
  String and Array[Byte] extend java.io.Serializable,
  so this implicit needs to be lower priority than those in BinaryPrimitiveCodecs
   */
  implicit def anyRefBinaryCodec[S <: java.io.Serializable](implicit ev: ClassTag[S]): Codec[S] =
    new JavaSerializationAnyRefCodec[S](ev)

}

trait BinaryAnyRefCodecs_0 extends BinaryAnyRefCodecs_1
