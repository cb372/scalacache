import scala.language.experimental.macros
import scala.concurrent.duration._

package object cacheable {

  /**
   * Perform the given operation and memoize its result to a cache before returning it.
   * If the result is already in the cache, return it without performing the operation.
   *
   * The result is stored in the cache without a TTL, so it will remain until it is naturally evicted.
   *
   * @param f function that returns some result. This result is the valued that will be cached.
   * @param cacheConfig cache configuration
   * @tparam A type of the value to be cached
   * @return the result, either retrieved from the cache or calculated by executing the function `f`
   */
  def cacheable[A](f: => A)(implicit cacheConfig: CacheConfig): A = macro Macros.cacheableImpl[A]

  /**
   * Perform the given operation and memoize its result to a cache before returning it.
   * If the result is already in the cache, return it without performing the operation.
   *
   * The result is stored in the cache with the given TTL. It will be evicted when the TTL is up.
   *
   * Note that if the result is currently in the cache, changing the TTL has no effect.
   * TTL is only set once, when the result is added to the cache.
   *
   * @param ttl Time To Live. How long the result should be stored in the cache.
   * @param f function that returns some result. This result is the valued that will be cached.
   * @param cacheConfig cache configuration
   * @tparam A type of the value to be cached
   * @return the result, either retrieved from the cache or calculated by executing the function `f`
   */
  def cacheable[A](ttl: Duration)(f: => A)(implicit cacheConfig: CacheConfig): A = macro Macros.cacheableImplWithTTL[A]


  /**
   * Remove the given method call's result from the cache.
   * A check will be performed (at compile time) to ensure that the cache key matches an actual method.
   *
   * e.g. to remove the result of `UserRepository.getUser(123)` from the cache,
   * you would call `invalidate(classOf[UserRepository], "getUser", Seq(Seq(123))`.
   *
   * @param clazz
   * @param cacheConfig
   * @return
   */
  def invalidate(clazz: Class[_], methodName: String, paramss: Seq[Seq[Any]])(implicit cacheConfig: CacheConfig): Unit = {
    // TODO make all of this a macro


    import scala.reflect.runtime.{universe => ru}

    def paramListsMatch(symbols: List[List[ru.Symbol]], paramss: Seq[Seq[Any]]): Boolean = {
      // TODO can check types are correct using <:<
      symbols.length == paramss.length && symbols.zip(paramss).forall { case (syms, params) => syms.length == params.length }
    }

    val m = ru.runtimeMirror(getClass.getClassLoader)
    val classType = m.staticClass(clazz.getCanonicalName).selfType
    val methodTerm = classType.decl(ru.TermName(methodName))
    if (methodTerm.isTerm) {
      val methodExists = methodTerm.alternatives.exists { sym => sym.isMethod && paramListsMatch(sym.asMethod.paramLists, paramss) }
      if (methodExists) {
        // Found a matching method, let's make a cache key and do the invalidation
        val cacheKey = cacheConfig.keyGenerator.toCacheKey(classType.termSymbol.fullName, methodName, paramss)
        println(s"Invalidating cache value with key $cacheKey")
        // TODO the actual invalidation
      }
    } else {
      throw new IllegalArgumentException(s"$methodName is not a method of class ${clazz.getCanonicalName}")
    }


  }
}

