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

package scalacache.serialization

import java.io.{InputStream, ObjectInputStream, ObjectStreamClass}

import scala.reflect.ClassTag
import scala.util.control.NonFatal

/** Object input stream which tries the thread local class loader.
  *
  * Thread Local class loader is used by SBT to avoid polluting system class loader when running different tasks.
  *
  * This allows deserialization of classes from sub-projects during something like Play's test/run modes.
  */
private[serialization] class GenericCodecObjectInputStream(classTag: ClassTag[_], in: InputStream)
    extends ObjectInputStream(in) {

  private def classTagClassLoader =
    classTag.runtimeClass.getClassLoader
  private def threadLocalClassLoader =
    Thread.currentThread().getContextClassLoader

  override protected def resolveClass(desc: ObjectStreamClass): Class[_] = {
    try classTagClassLoader.loadClass(desc.getName)
    catch {
      case NonFatal(_) =>
        try super.resolveClass(desc)
        catch {
          case NonFatal(_) =>
            threadLocalClassLoader.loadClass(desc.getName)
        }
    }
  }
}
