---
layout: docs
title: Flags
---

### Flags

Cache GETs and/or PUTs can be temporarily disabled using flags. This can be useful if for example you want to skip the cache and read a value from the DB under certain conditions.

You can set flags by defining a [scalacache.Flags](https://github.com/cb372/scalacache/blob/master/modules/core/src/main/scala/scalacache/Flags.scala) instance in implicit scope.

The detailed behaviour of the flags is as follows:

* If `readsEnabled` = false, the cache will not be read, and ScalaCache will behave as if it was a cache miss. This means that memoization will compute the value (e.g. read it from a DB) and then write it to the cache.
* If `writesEnabled` = false, in the case of a cache miss, the value will be computed (e.g. read from a DB) but it will not be written to the cache.
* If both flags are false, memoization will not read from the cache or write to the cache.

Note that your memoized method must take an implicit parameter of type `Flags`. Otherwise any flags you try to set using an implicit will be silently ignored.

Example:

```scala mdoc:silent:reset-object
import scalacache._
import scalacache.memcached._
import scalacache.memoization._
import scalacache.serialization.binary._
import cats.effect.IO
import cats.effect.unsafe.implicits.global

final case class Cat(id: Int, name: String, colour: String)

implicit val catsCache: Cache[IO, String, Cat] = MemcachedCache("localhost:11211")

def getCatWithFlags(id: Int)(implicit flags: Flags): Cat = memoize(None) {
  // Do DB lookup here...
  Cat(id, s"cat ${id}", "black")
}.unsafeRunSync()

def getCatMaybeSkippingCache(id: Int, skipCache: Boolean): Cat = {
  implicit val flags = Flags(readsEnabled = !skipCache)
  getCatWithFlags(id)
}
```

Tip: Because the flags are passed as a parameter to your method, they will be included in the generated cache key. This means the cache key will vary depending on the value of the flags, which is probably not what you want. In that case, you should exclude the `implicit flags: Flags` parameter from cache key generation by annotating it with `@cacheKeyExclude`.

```scala mdoc:invisible
for (cache <- List(catsCache)) {
  cache.close.unsafeRunSync()
} 
```
