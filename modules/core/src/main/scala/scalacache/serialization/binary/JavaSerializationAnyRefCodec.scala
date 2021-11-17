/*
 * Copyright 2021 scalacache
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package scalacache.serialization.binary

import java.io._

import scala.reflect.ClassTag
import scala.util.control.NonFatal
import scalacache.serialization.Codec.DecodingResult
import scalacache.serialization.{Codec, GenericCodecObjectInputStream}

/** Codec that uses Java serialization to serialize objects
  *
  * Credit: Shade @ https://github.com/alexandru/shade/blob/master/src/main/scala/shade/memcached/Codec.scala
  */
class JavaSerializationAnyRefCodec[S <: Serializable](classTag: ClassTag[S]) extends BinaryCodec[S] {

  def using[T <: Closeable, R](obj: T)(f: T => R): R =
    try f(obj)
    finally try obj.close()
    catch {
      case NonFatal(_) => // does nothing
    }

  def encode(value: S): Array[Byte] =
    using(new ByteArrayOutputStream()) { buf =>
      using(new ObjectOutputStream(buf)) { out =>
        out.writeObject(value)
        out.close()
        buf.toByteArray
      }
    }

  def decode(data: Array[Byte]): DecodingResult[S] =
    Codec.tryDecode {
      using(new ByteArrayInputStream(data)) { buf =>
        val in = new GenericCodecObjectInputStream(classTag, buf)
        using(in) { inp =>
          inp.readObject().asInstanceOf[S]
        }
      }
    }
}
