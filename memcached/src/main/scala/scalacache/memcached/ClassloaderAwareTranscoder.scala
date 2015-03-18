package scalacache.memcached

import java.io.{ ObjectInputStream, ByteArrayInputStream, IOException }

import net.spy.memcached.compat.CloseUtil
import net.spy.memcached.transcoders.SerializingTranscoder

import scalacache.util.ClassLoaderOIS

class ClassloaderAwareTranscoder(classloader: ClassLoader) extends SerializingTranscoder {

  /**
   * Copied from parent class and changed to use ClassLoaderOIS
   */
  override def deserialize(in: Array[Byte]): AnyRef = {
    var rv: AnyRef = null
    var bis: ByteArrayInputStream = null
    var is: ObjectInputStream = null
    try {
      if (in != null) {
        bis = new ByteArrayInputStream(in)
        is = new ClassLoaderOIS(bis, classloader)
        rv = is.readObject
        is.close()
        bis.close()
      }
    } catch {
      case e: IOException =>
        val bytesLength: Int = if (in == null) 0 else in.length
        getLogger.warn("Caught IOException decoding %d bytes of data", bytesLength.toString, e)
      case e: ClassNotFoundException =>
        val bytesLength: Int = if (in == null) 0 else in.length
        getLogger.warn("Caught CNFE decoding %d bytes of data", bytesLength.toString, e)
    } finally {
      CloseUtil.close(is)
      CloseUtil.close(bis)
    }
    rv
  }

}
