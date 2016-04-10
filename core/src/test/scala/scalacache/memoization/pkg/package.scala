package scalacache.memoization

import scalacache._
import scalacache.serialization.InMemoryRepr

package object pkg {
  implicit var scalaCache: ScalaCache[InMemoryRepr] = null

  def insidePackageObject(a: Int): Int = memoizeSync {
    123
  }

}
