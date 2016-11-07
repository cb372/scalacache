package scalacache

/**
 * Configuration flags for conditionally altering the behaviour of ScalaCache.
 * <p>
 * The difference between `invalidateEnabled = true` and `readsEnabled = false` is that
 * the former guarantees that the new value is available in the cache once the `Future` is complete
 * whereas the latter doesn't, in other words, writing to the cache can be still in progress.
 *
 * @param readsEnabled if false, cache GETs will be skipped (and will return `None`)
 * @param writesEnabled if false, cache PUTs will be skipped
 * @param invalidateEnabled if true, invalidate the cache. if true, `readsEnabled` and `writesEnabled` are ignored.
 */
case class Flags(readsEnabled: Boolean = true,
                 writesEnabled: Boolean = true,
                 invalidateEnabled: Boolean = false)

object Flags {

  /**
   * The default flag values. These can be overriden at the call site, e.g.
   *
   * {{{
   *   def foo() {
   *     implicit val myCustomFlags = Flags(...)
   *     val cachedValue = scalacache.get("wow")
   *     ...
   *   }
   * }}}
   */
  implicit val defaultFlags = Flags()

}
