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
