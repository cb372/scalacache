0.9.3 (2016/11/08)
----

New features:

* ScalaCache is now published for Scala 2.11.x and 2.12.0
* scalacache-core is also published for Scala.js (thanks to @mdedetrich). On its own, this is not very useful, but it paves the way for proper Scala.js support in the future.

Change in behaviour:

The write semantics of the `caching` method (and, by extension, memoization) is now configurable. By default, in the case of a cache miss, the `Future` returned from the method will not complete until the cache write has completed.

This makes the behaviour deterministic, as you have a guarantee that the value is present in the cache once the Future completes. 

If you want to maintain ScalaCache's old behaviour (i.e. complete the `Future` as soon as the value is computed, then perform the cache write in an asynchronous callback), you can toggle the new `waitForWriteToComplete` flag in `CacheConfig`.

Other stuff:

* Bump sbt 0.13.13
* Documentation fixes

0.9.2 (2016/09/23)
----

New features:

* ScalaCache is now published for Scala 2.11.x and 2.12.0-RC1

Other stuff:

* Bump sbt 0.13.12 and move from `Build.scala` to `build.sbt`
* Minor version bumps: Guava, Spymemcached, Ehcache, Jedis, Caffeine, joda-time, slf4j

0.9.1 (2016/05/30)
----

New features:

* ScalaCache is now published for Scala 2.11.x and 2.12.0-M4

0.9.0 (2016/05/30)
----

New features:

* Add a new `MethodCallToStringConverter` implementation

Improvements:

* Performance improvements [#102](https://github.com/cb372/scalacache/pull/102), [#103](https://github.com/cb372/scalacache/pull/103) 

Breaking changes:

* Removed support for Twitter Util LruMap as part of the preparation for Scala 2.12.x. If this is a problem for you, please open an issue.

Other stuff:

* Added some JMH benchmarks
* Bumped Caffeine to 2.3.0

0.8.1 (2016/04/11)
----

New features:

* [#89](https://github.com/cb372/scalacache/pull/89) adds support for compression in Memcached/Redis. Thanks to @lloydmeta. See https://github.com/cb372/scalacache#compression-of-codeca-arraybyte for more details on how to enable compression.

Bug fixes:

* [#92](https://github.com/cb372/scalacache/pull/92) fixes issue [#90](https://github.com/cb372/scalacache/issues/90). This was a regression caused by the introduction of custom serialization in 0.8.0, making it impossible to cache any object that did not extend `java.io.Serializable`.

Breaking changes:

Sorry, this version introduces a small potentially breaking change. Some methods now have an extra type parameter to specify how your data is represented after serialization. If you are using Memcached or Redis, this will be `Array[Byte]`. If you are using an in-memory cache such as Guava, EhCache or Caffeine, then you don't need serialization, so it will be the special type `NoSerialization`.

Depending on how you are using ScalaCache, you may need to manually specify this type parameter in your code, although it should be unnecessary in most cases.

e.g. if your code looks like this:

```scalacache
import scalacache._

implicit val scalaCache = ...

// Using the untyped API
val futureOfString = caching[String]("key")(doSomething())

// Using the typed API
val stringsCache = typed[String]
val futureOfOptionOfString = stringsCache.get("key")
```

then it may need to be updated to look like this:

```scalacache
import scalacache._

implicit val scalaCache = ...

// Using the untyped API
val futureOfString = caching[String, NoSerialization]("key")(doSomething()) // assuming an in-memory cache
//val futureOfString = caching[String, Array[Byte]]("key")(doSomething()) // use Array[Byte] if you are using Memcached or Redis

// Using the typed API
val stringsCache = typed[String, NoSerialization] // assuming an in-memory cache
val futureOfOptionOfString = stringsCache.get("key")
```

0.8.0 (2016/04/03)
----

New features:

* [#86](https://github.com/cb372/scalacache/pull/86) adds support for custom serialisation. Thanks to @lloydmeta

  Note that the addition of this feature may require you to add type annotations in your code in places where you are
  getting items from the cache (e.g. `get[String]("foo")` instead of `get("foo")`).

Breaking changes:

This release changes the serialisation format used in the Memcached and Redis cache implementations.
If you are using `scalacache-memcached` or `scalacache-redis`, you will not be able to retrieve any data
that you inserted into the cache using ScalaCache 0.7.x or earlier.

If you want to avoid this problem, you can set the `useLegacySerialization` flag in the cache's constructor.
This will preserve the ScalaCache 0.7.x behaviour, using the underlying library's mechanism for serialisation
instead of the new typeclass-based mechanism.

0.7.5 (2015/12/10)
----

New features:

* [#81](https://github.com/cb372/scalacache/pull/81) adds support for sharded Redis and Redis Sentinel. Thanks to @jareddellitt and @ctblog for the original PRs (#66 and #78) that inspired this.

Improvements:

* Added support for passing `Duration.Inf` or `Duration.Undefined` as a TTL. It is interpreted as meaning "cache with no TTL".

Other stuff:

* Bumped Jedis version from 2.7.2 to 2.8.0.

0.7.4 (2015/11/22)
----

Improvements:

* Bumped Caffeine to v2.0.1. Version 2 of Caffeine contains [performance and cache efficiency improvments](https://github.com/ben-manes/caffeine/releases/tag/v2.0.0).

Bug fixes:

* [#73](https://github.com/cb372/scalacache/pull/73) fixes a typo in the new variant of `memoize` that was added in #72 (thanks to @mdedetrich)

0.7.3 (2015/11/09)
----

Improvements:

* [#72](https://github.com/cb372/scalacache/pull/72) adds some convenience methods that take an optional TTL as an `Option[Duration]` (thanks to @mdedetrich)

0.7.2 (2015/10/29)
----

New features:

* [#71](https://github.com/cb372/scalacache/pull/71) adds a new cache implementation, [Caffeine](https://github.com/ben-manes/caffeine) (thanks to @mchv)

0.7.1 (2015/10/27)
----

Bug fixes:

* [#69](https://github.com/cb372/scalacache/pull/69) fixes an integer overflow issue when using a TTL longer than Int.MaxValue milliseconds (about 23 days) with Guava.

Other stuff:

* Bumped the sbt plugins. Some of them were pretty ancient.

0.7.0 (2015/09/27)
----

New features:

* [#55](https://github.com/cb372/scalacache/pull/55) adds a Typed API, so you can ensure you don't accidentally read/write something of the wrong type. See the README for more details.
* [#62](https://github.com/cb372/scalacache/pull/62) adds asynchronous versions of `caching` and `memoize`. See the README for more details.

Breaking API changes (sorry!):

 #62 resulted in a few breaking changes, as follows:

* `scalacache.caching` and `scalacache.cachingWithTTL` have been renamed to `scalacache.sync.caching` and `scalacache.sync.cachingWithTTL` respectively. This is to make way for the new asynchronous versions of these methods.
* `scalacache.getSync` has been deprecated and will be removed soon. Please use `scalacache.sync.get` instead.
* Both of the overloaded `scalacache.memoization.memoize` methods have been renamed to `scalacache.memoization.memoizeSync`. This is to make way for the new asynchronous versions of these methods.

Other stuff:

* README fixes
* Bump Scala, sbt
* Use macro bundles, thanks to @philwills

0.6.4 (2015/07/10)
----

Improvements:

* [#50](https://github.com/cb372/scalacache/pull/50) adds a `removeAll()` method to the `Cache` class, so you can flush the entire contents of the cache.

Bumped dependencies:

* Scala 2.11.7

0.6.3 (2015/05/17)
----

Improvements:

* [#49](https://github.com/cb372/scalacache/pull/49) adds a `close()` method to the `Cache` class, so you can clean up resources properly (i.e. close the Memcached/Redis client) when your app shuts down.

Bumped dependencies:

* Ehcache 2.8.4 -> 2.10.0
* Spymemcached 2.11.4 -> 2.11.7
* Jedis 2.6.0 -> 2.7.2

0.6.2 (2015/04/08)
----

New features:

* [#41](https://github.com/cb372/scalacache/pull/41) (Add a wrapper for twitter-util's LruMap cache implementation)

0.6.1 (2015/04/01)
----

Improvements:

* [#40](https://github.com/cb372/scalacache/pull/40) (Add exception handling to `memoize` blocks, so exceptions thrown by the cache implementation are caught and handled gracefully. See [#39](https://github.com/cb372/scalacache/issues/39) for the bug report)

0.6.0 (2015/03/14)
----

Improvements:

* [#37](https://github.com/cb372/scalacache/issues/37) (make it possible to use scalacache-redis with Play. See [#32](https://github.com/cb372/scalacache/issues/32) for the bug report)
* [#38](https://github.com/cb372/scalacache/issues/38) (make `RedisCache` thread-safe)

Breaking changes:

* Due to #38, `RedisCache` now takes a `JedisPool` in its constructor rather than a `Jedis`.

0.5.2 (2015/02/10)
----

New features:
 
* [#31](https://github.com/cb372/scalacache/pull/31) (added `@cacheKeyExclude` annotation, as suggested in [#30](https://github.com/cb372/scalacache/issues/30))

0.5.1 (2015/02/09)
----

Improvements:
 
* [#29](https://github.com/cb372/scalacache/pull/29) (upgrade scala-logging in order to fix [#28](https://github.com/cb372/scalacache/issues/28))

0.5.0 (2014/12/23)
----

Improvements:
 
* [#21](https://github.com/cb372/scalacache/issues/21) (as suggested in [#17](https://github.com/cb372/scalacache/issues/17))

Misc:

* Add Coveralls badge
* Start using sbt-release plugin for releases

0.4.2 (2014/10/26)
----

Improvements:
 
* [#17](https://github.com/cb372/scalacache/issues/17)

0.4.1 (2014/10/19)
----

Bugfixes:
 
* [#13](https://github.com/cb372/scalacache/issues/13)
* [#14](https://github.com/cb372/scalacache/issues/14)

0.4.0 (2014/10/09)
----

* New feature: conditionally disable caching using flags, as suggested in [#10](https://github.com/cb372/scalacache/issues/10)
* Upgrade dependencies

Bumped the minor version because signatures of public methods have changed slightly.

0.3.2 (2014/08/29)
----

* PR #8 adds a better key sanitizer for Memcached (thanks to @lloydmeta)

0.3.1 (2014/07/25)
----

* Upgrade to Scala 2.11.2

0.3.0 (2014/05/08)
----

* Change project name: Cacheable is now ScalaCache! (Sorry, lots of breaking API changes)
* Switch Redis implementation from Scala-Redis to Jedis.
* Async support (cache operations now return Futures).
* Helper to build cache keys from multiple parts
* Support for automatically adding a prefix to all keys

0.2.0 (2014/04/20)
----

Scala 2.11 support
Bump dependency versions

0.1.1 (2014/01/14)
----

Fix sbbt dependency (#3)

0.1 (2013/11/17)
-----

First release
