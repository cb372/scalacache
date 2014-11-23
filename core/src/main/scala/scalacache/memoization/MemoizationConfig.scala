package scalacache.memoization

/**
 * Configuration related to the behaviour of the [[scalacache.memoization.memoize()]] methods.
 *
 * @param toStringConvertor convertor for generating a String cache key from information about a method call
 */
case class MemoizationConfig(toStringConvertor: MethodCallToStringConvertor = MethodCallToStringConvertor.defaultConvertor)

