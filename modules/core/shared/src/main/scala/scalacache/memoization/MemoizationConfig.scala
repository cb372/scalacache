package scalacache.memoization

/**
 * Configuration related to the behaviour of the `scalacache.memoization.memoize{Sync}` methods.
 *
 * @param toStringConverter converter for generating a String cache key from information about a method call
 */
case class MemoizationConfig(toStringConverter: MethodCallToStringConverter = MethodCallToStringConverter.excludeClassConstructorParams)

