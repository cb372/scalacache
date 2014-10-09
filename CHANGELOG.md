0.4.0 (2014/10/09)
----

* New feature: conditionally disable caching using flags, as suggested in #10
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
