package scalacache.memoization

import scalacache._

package object pkg {
  implicit var scalaCache: ScalaCache = null

  def insidePackageObject(a: Int): Int = memoize {
    123
  }

}
