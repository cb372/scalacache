package cacheable.redis

import java.io._
import com.redis.serialization.{Parse, Format}

/**
 * Custom serialization for caching arbitrary objects in Redis.
 * Ints, Longs, Doubles, Strings and byte arrays are treated specially.
 * Everything else is serialized using standard Java serialization.
 *
 * Author: chris
 * Created: 11/17/13
 */
trait RedisSerialization {

  object MagicNumbers {
    val MARKER: Array[Byte] = Array(0xF.toByte, 0xE.toByte, 0xD.toByte, 0xC.toByte)
    val STRING: Byte = 0
    val BYTE_ARRAY: Byte = 1
    val DOUBLE: Byte = 2
    val LONG: Byte = 3
    val OBJECT: Byte = 4
  }

  def withObjectOutputStream(typeId: Byte, f: ObjectOutputStream => Unit): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    baos.write(MagicNumbers.MARKER) // Write the marker
    baos.write(typeId) // Write the type ID
    val oos = new ObjectOutputStream(baos) // Write the rest of the array in ObjectOutputStream format
    f(oos)
    oos.flush()
    baos.toByteArray
  }

  implicit val format: Format = Format {
    /*
     We have to format Ints as-is, because the formatter is used for serializing TTL as well as value,
     and Redis expects the TTL to be a numeric string, e.g. "123".

     For all other types, we output a marker (to show that it's not an Int),
     followed by one byte to say what type of data it is,
     and finally the data itself.
      */
    case i: Int => { i.toString.getBytes("UTF-8") }
    case s: String => withObjectOutputStream(MagicNumbers.STRING, _.writeUTF(s))
    case bs: Array[Byte] => withObjectOutputStream(MagicNumbers.BYTE_ARRAY, { oos =>
      oos.writeInt(bs.length)
      oos.write(bs)
    })
    case d: Double => withObjectOutputStream(MagicNumbers.DOUBLE, _.writeDouble(d))
    case l: Long => withObjectOutputStream(MagicNumbers.LONG, _.writeLong(l))
    case any => withObjectOutputStream(MagicNumbers.OBJECT, _.writeObject(any))
  }

  implicit def parse[A]: Parse[A] = Parse { (bytes: Array[Byte]) =>
    if (bytes.length > 4 &&
      bytes(0) == MagicNumbers.MARKER(0) &&
      bytes(1) == MagicNumbers.MARKER(1) &&
      bytes(2) == MagicNumbers.MARKER(2) &&
      bytes(3) == MagicNumbers.MARKER(3)) {

      // It's not an Int, so do custom deserialization
      deserialize(bytes).asInstanceOf[A]
    } else {
      // No marker, so it's an Int
      new String(bytes, "UTF-8").toInt.asInstanceOf[A]
    }
  }

  def deserialize(bytes: Array[Byte]) = {
    val bais = new ByteArrayInputStream(bytes)
    bais.skip(MagicNumbers.MARKER.length) // Skip the marker
    val typeId = bais.read().toByte // Read the next byte to discover the type
    val ois = new ObjectInputStream(bais) // The rest of the array is in ObjectInputStream format
    typeId match {
      case MagicNumbers.STRING => ois.readUTF()
      case MagicNumbers.BYTE_ARRAY => {
        val len = ois.readInt()
        val bs = new Array[Byte](len)
        ois.read(bs)
        bs
      }
      case MagicNumbers.DOUBLE => ois.readDouble()
      case MagicNumbers.LONG => ois.readLong()
      case MagicNumbers.OBJECT => ois.readObject()
    }
  }

}
