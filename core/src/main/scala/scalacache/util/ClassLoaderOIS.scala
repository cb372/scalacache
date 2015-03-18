package scalacache.util

import java.io.{ InputStream, ObjectInputStream, ObjectStreamClass }

class ClassLoaderOIS(stream: InputStream, classloader: ClassLoader) extends ObjectInputStream(stream) {
  override protected def resolveClass(desc: ObjectStreamClass) = {
    Class.forName(desc.getName, false, classloader)
  }
}
