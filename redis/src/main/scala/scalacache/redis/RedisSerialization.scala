package scalacache.redis

import java.io._

/**
 * Custom serialization for caching arbitrary objects in Redis.
 * Ints, Longs, Doubles, Strings and byte arrays are treated specially.
 * Everything else is serialized using standard Java serialization.
 */
trait RedisSerialization {

  protected def customClassloader: Option[ClassLoader] = None

  object MagicNumbers {
    val STRING: Byte = 0
    val BYTE_ARRAY: Byte = 1
    val INT: Byte = 2
    val DOUBLE: Byte = 3
    val LONG: Byte = 4
    val OBJECT: Byte = 5
  }

  def withObjectOutputStream(typeId: Byte, f: ObjectOutputStream => Unit): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    baos.write(typeId) // Write the type ID
    val oos = new ObjectOutputStream(baos) // Write the rest of the array in ObjectOutputStream format
    f(oos)
    oos.flush()
    baos.toByteArray
  }

  def serialize(value: Any): Array[Byte] = value match {
    case s: String => withObjectOutputStream(MagicNumbers.STRING, _.writeUTF(s))
    case bs: Array[Byte] => withObjectOutputStream(MagicNumbers.BYTE_ARRAY, { oos =>
      oos.writeInt(bs.length)
      oos.write(bs)
    })
    case i: Int => withObjectOutputStream(MagicNumbers.INT, _.writeInt(i))
    case d: Double => withObjectOutputStream(MagicNumbers.DOUBLE, _.writeDouble(d))
    case l: Long => withObjectOutputStream(MagicNumbers.LONG, _.writeLong(l))
    case any => withObjectOutputStream(MagicNumbers.OBJECT, _.writeObject(any))
  }

  def deserialize[A](bytes: Array[Byte]): A = {
    val bais = new ByteArrayInputStream(bytes)
    val typeId = bais.read().toByte // Read the next byte to discover the type
    val ois = createObjectInputStream(bais) // The rest of the array is in ObjectInputStream format
    val result = typeId match {
      case MagicNumbers.STRING => ois.readUTF()
      case MagicNumbers.BYTE_ARRAY => {
        val len = ois.readInt()
        val bs = new Array[Byte](len)
        ois.read(bs)
        bs
      }
      case MagicNumbers.INT => ois.readInt()
      case MagicNumbers.DOUBLE => ois.readDouble()
      case MagicNumbers.LONG => ois.readLong()
      case MagicNumbers.OBJECT => ois.readObject()
    }
    result.asInstanceOf[A]
  }

  private def createObjectInputStream(inputStream: InputStream): ObjectInputStream = customClassloader match {
    case Some(classloader) => new ClassLoaderOIS(inputStream, classloader)
    case None => new ObjectInputStream(inputStream)
  }

}

class ClassLoaderOIS(stream: InputStream, customClassloader: ClassLoader) extends ObjectInputStream(stream) {
  override protected def resolveClass(desc: ObjectStreamClass) = {
    Class.forName(desc.getName, false, customClassloader)
  }
}
