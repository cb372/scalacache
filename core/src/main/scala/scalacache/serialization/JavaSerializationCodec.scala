package scalacache.serialization

import java.io._

import scala.reflect.ClassTag
import scala.util.control.NonFatal

/**
  * The world's least useful codec.
  * Just throws a runtime exception if you try to use it.
  *
  * Only exists so that we can provide codecs for any class <: AnyRef,
  * so that people don't run into "codec missing" issues when using in-memory cache impls e.g. Caffeine.
  *
  * See https://github.com/cb372/scalacache/issues/90
  */
trait AnyRefDummyLowPriorityCodec {

  implicit def AnyRefBinaryCodec[S <: AnyRef](implicit classTag: ClassTag[S]) = new Codec[S] {

    def serialize(value: S): Array[Byte] =
      sys.error(s"Sorry, I don't know how to serialize an instance of class ${classTag.runtimeClass.getName}. Please provide a custom Codec.")

    def deserialize(data: Array[Byte]): S =
      sys.error(s"Sorry, I don't know how to deserialize an instance of class ${classTag.runtimeClass.getName}. Please provide a custom Codec.")

  }

}

/**
  * Holds a Java-serialisation-based Codec[Object <: Serializable] instance
  *
  * Credit: Shade @ https://github.com/alexandru/shade/blob/master/src/main/scala/shade/memcached/Codec.scala
  */
trait JavaSerializationCodec extends AnyRefDummyLowPriorityCodec {

  private[this] class GenericCodec[S <: Serializable](classTag: ClassTag[S]) extends Codec[S] {

    def using[T <: Closeable, R](obj: T)(f: T => R): R =
      try
        f(obj)
      finally
        try obj.close() catch {
          case NonFatal(_) => // does nothing
        }

    def serialize(value: S): Array[Byte] =
      using (new ByteArrayOutputStream()) { buf =>
        using (new ObjectOutputStream(buf)) { out =>
          out.writeObject(value)
          out.close()
          buf.toByteArray
        }
      }

    def deserialize(data: Array[Byte]): S =
      using (new ByteArrayInputStream(data)) { buf =>
        val in = new GenericCodecObjectInputStream(classTag, buf)
        using (in) { inp =>
          inp.readObject().asInstanceOf[S]
        }
      }
  }

  /**
    * Uses plain Java serialization to deserialize objects
    */
  implicit def SerializableBinaryCodec[S <: Serializable](implicit ev: ClassTag[S]): Codec[S] =
    new GenericCodec[S](ev)

}

/**
  * Object input stream which tries the thread local class loader.
  *
  * Thread Local class loader is used by SBT to avoid polluting system class loader when
  * running different tasks.
  *
  * This allows deserialization of classes from sub-projects during something like
  * Play's test/run modes.
  */
class GenericCodecObjectInputStream(classTag: ClassTag[_], in: InputStream)
  extends ObjectInputStream(in) {

  private def classTagClassLoader =
    classTag.runtimeClass.getClassLoader
  private def threadLocalClassLoader =
    Thread.currentThread().getContextClassLoader

  override protected def resolveClass(desc: ObjectStreamClass): Class[_] = {
    try classTagClassLoader.loadClass(desc.getName) catch {
      case NonFatal(_) =>
        try super.resolveClass(desc) catch {
          case NonFatal(_) =>
            threadLocalClassLoader.loadClass(desc.getName)
        }
    }
  }
}
