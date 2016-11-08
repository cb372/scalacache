package scalacache.serialization

import java.io.Serializable
import scala.reflect.ClassTag

object GZippingJavaAnyBinaryCodec {

  /**
    * Compressing Java generic codec with a threshold of 16K
    */
  implicit def default[S <: Serializable](implicit ev: ClassTag[S]): GZippingJavaAnyBinaryCodec[S] = new GZippingJavaAnyBinaryCodec(CompressingCodec.DefaultSizeThreshold)(ev)

}

class GZippingJavaAnyBinaryCodec[S <: Serializable](override val sizeThreshold: Int)(implicit classTag: ClassTag[S])
  extends JavaSerializationAnyCodec[S](classTag)
    with GZippingBinaryCodec[S]
