package scalacache.serialization.gzip

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import scalacache.serialization.Codec.DecodingResult
import scalacache.serialization.{Codec, FailedToDecode}
import scodec.bits.ByteVector

object CompressingCodec {

  /**
    * Default threshold for compression is is 16k
    */
  final val DefaultSizeThreshold: Int = 16384

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
trait GZippingBinaryCodec[A] extends Codec[A] {

  import CompressingCodec._

  /**
    * Size above which data will get compressed
    */
  protected val sizeThreshold: Int = CompressingCodec.DefaultSizeThreshold

  abstract override def encode(value: A): ByteVector = {
    val serialised = super.encode(value)
    if (serialised.length > sizeThreshold) {
      Headers.Gzipped +: compress(serialised)
    } else {
      Headers.Uncompressed +: serialised
    }
  }

  abstract override def decode(data: ByteVector): DecodingResult[A] = {
    val firstByte = data.headOption
    firstByte match {
      case Some(Headers.Uncompressed) =>
        super.decode(data.tail)
      case Some(Headers.Gzipped) =>
        val bytes = Codec.tryDecode(decompress(data))
        bytes.right.flatMap(super.decode)
      case unexpected =>
        Left(
          FailedToDecode(
            new RuntimeException(s"Expected either ${Headers.Uncompressed} or ${Headers.Gzipped} but got $unexpected")))
    }
  }

  // Port of compress in SpyMemcached
  private final def compress(data: ByteVector): ByteVector = {
    val byteOutputStream = new ByteArrayOutputStream()
    val gzipOutputStream = new GZIPOutputStream(byteOutputStream)
    try {
      gzipOutputStream.write(data.toArray)
    } finally {
      gzipOutputStream.close()
      byteOutputStream.close()
    }
    ByteVector(byteOutputStream.toByteArray)
  }

  // Port of decompress in SpyMemcached
  private final def decompress(data: ByteVector): ByteVector = {
    val bytesArray = data.toArray
    val bis = new ByteArrayInputStream(bytesArray, 1, bytesArray.length - 1)
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
    ByteVector(bos.toByteArray)
  }
}
