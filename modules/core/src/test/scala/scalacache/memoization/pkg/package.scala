package scalacache.memoization

import scalacache._

package object pkg {

  import cats.effect.SyncIO

  implicit var cache: Cache[SyncIO, String, Int] with MemoizingCache[SyncIO, Int] = null

  def insidePackageObject(a: Int): SyncIO[Int] = memoize(None) {
    123
  }

}
