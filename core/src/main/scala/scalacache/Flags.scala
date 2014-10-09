package scalacache

/**
 * Configuration flags for conditionally altering the behaviour of ScalaCache.
 *
 * @param readsEnabled if false, cache GETs will be skipped (and will return `None`)
 * @param writesEnabled if false, cache PUTs will be skipped
 */
case class Flags(readsEnabled: Boolean = true,
                 writesEnabled: Boolean = true)

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
