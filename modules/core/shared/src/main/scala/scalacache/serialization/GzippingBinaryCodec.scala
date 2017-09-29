package scalacache.serialization

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

object CompressingCodec {

  /**
    * Default threshold for compression is is 16k
    */
  val DefaultSizeThreshold: Int = 16384

  /**
    * Headers aka magic numbers to let us know if something has been compressed or not
    */
  object Headers {
    val Uncompressed: Byte = 0
    val Gzipped: Byte = 1
  }

}

/**
  * Mixing this into any Codec will automatically GZip the resulting Byte Array when serialising and handle un-Gzipping when
  * deserialising
  */
trait GZippingBinaryCodec[A] extends Codec[A, Array[Byte]] {

  import CompressingCodec._

  /**
    * Size above which data will get compressed
    */
  protected def sizeThreshold: Int = CompressingCodec.DefaultSizeThreshold

  abstract override def serialize(value: A): Array[Byte] = {
    val serialised = super.serialize(value)
    if (serialised.length > sizeThreshold) {
      Headers.Gzipped +: compress(serialised)
    } else {
      Headers.Uncompressed +: serialised
    }
  }

  abstract override def deserialize(data: Array[Byte]): A = {
    val firstByte = data.headOption
    firstByte match {
      case Some(Headers.Uncompressed) => super.deserialize(data.tail)
      case Some(Headers.Gzipped) => super.deserialize(decompress(data.tail))
      case unexpected =>
        throw new RuntimeException(
          s"Expected either ${Headers.Uncompressed} or ${Headers.Gzipped} but got $unexpected")
    }
  }

  // Port of compress in SpyMemcached
  private def compress(data: Array[Byte]): Array[Byte] = {
    val byteOutputStream = new ByteArrayOutputStream()
    val gzipOutputStream = new GZIPOutputStream(byteOutputStream)
    try {
      gzipOutputStream.write(data)
    } finally {
      gzipOutputStream.close()
      byteOutputStream.close()
    }
    byteOutputStream.toByteArray
  }

  // Port of decompress in SpyMemcached
  private def decompress(data: Array[Byte]): Array[Byte] = {
    val bis = new ByteArrayInputStream(data)
    val gis = new GZIPInputStream(bis)
    val bos = new ByteArrayOutputStream
    val buf = new Array[Byte](4 * 1024)
    try {
      var r = gis.read(buf)
      while (r > 0) {
        bos.write(buf, 0, r)
        r = gis.read(buf)
      }
    } finally {
      gis.close()
      bis.close()
      bos.close()
    }
    bos.toByteArray
  }
}
