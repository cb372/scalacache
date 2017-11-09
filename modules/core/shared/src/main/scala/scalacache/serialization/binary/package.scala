package scalacache.serialization

import scala.reflect.ClassTag

package object binary extends BinaryPrimitiveCodecs {

  implicit def AnyRefBinaryCodec[S <: Serializable](implicit ev: ClassTag[S]): Codec[S] =
    new JavaSerializationAnyRefCodec[S](ev)

}
