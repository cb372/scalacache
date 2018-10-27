package scalacache.serialization.gzip

import java.io.Serializable

import scala.reflect.ClassTag
import scalacache.serialization.binary.JavaSerializationAnyRefCodec

object GZippingJavaSerializationAnyRefCodec {

  /**
    * Compressing Java generic codec with a threshold of 16K
    */
  implicit def default[S <: Serializable](implicit ev: ClassTag[S]): GZippingJavaSerializationAnyRefCodec[S] =
    new GZippingJavaSerializationAnyRefCodec(CompressingCodec.DefaultSizeThreshold)(ev)

}

class GZippingJavaSerializationAnyRefCodec[S <: Serializable](override val sizeThreshold: Int)(
    implicit classTag: ClassTag[S])
    extends JavaSerializationAnyRefCodec[S](classTag)
    with GZippingBinaryCodec[S]
