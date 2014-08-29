# ScalaCache

[![Build Status](https://travis-ci.org/cb372/scalacache.png)](https://travis-ci.org/cb372/scalacache)

(formerly known as Cacheable)

A facade for the most popular cache implementations, with a simple, idiomatic Scala API.

Use ScalaCache to add caching to any Scala app with the minimum of fuss.

The following cache implementations are supported, and it's easy to plugin your own implementation:
* Google Guava
* Memcached
* Ehcache
* Redis

## Versioning

Because of the use of Scala macros, only specific Scala versions are supported:

<table>
  <tr><th>ScalaCache</th><th>Scala</th><th>Notes</th></tr>
  <tr><td>0.1.x</td><td>2.10.3</td><td>artifactId = cacheable (the previous name of this project)</td></tr>
  <tr><td>0.2.x</td><td>2.11.0</td><td></td></tr>
  <tr><td>0.3.x</td><td>2.11.0, 2.11.1, 2.11.2</td><td></td></tr>
</table>

## How to use

### ScalaCache instance

To use ScalaCache you must first create a `ScalaCache` instance and ensure it is in implicit scope.
The `ScalaCache` is a container for the cache itself, as well as a variety of configuration parameters.
It packages up everything needed for caching into one case class for easy implicit passing.

The simplest way to construct a `ScalaCache` is just to pass a cache instance, like this:

```scala
import scalacache._

implicit val scalaCache = ScalaCache(new MyCache())
```

Note that depending on your cache implementation, the cache may take an `ExecutionContext` in its constructor. By default it will use `ExecutionContext.global`, but you can pass in a custom one if you wish.

### Basic cache operations

Assuming you have a `ScalaCache` in implicit scope:

```scala
import scalacache._

// Add an item to the cache
put("myKey")("myValue")

// Retrieve the added item
get("myKey") // Some(myValue)

// Remove it from the cache
remove("myKey")

// Wrap any block with caching
val result = caching("myKey") {
  // do stuff...
  "result of block"
}

// You can also pass multiple parts to be combined into one key
put("foo", 123, "bar")(value) // Will be cached with key "foo:123:bar"
```

### Memoization of method results

```scala 
import scalacache._
import memoization._

implicit val scalaCache = ScalaCache(new MyCache())

def getUser(id: Int): User = memoize {
  // Do DB lookup here...
  User(id, s"user${id}")
}
```

Did you spot the magic word 'memoize' in the `getUser` method? Just adding this keyword will cause the result of the method to be memoized to a cache.
The next time you call the method with the same arguments the result will be retrieved from the cache and returned immediately.

#### Time To Live

You can optionally specify a Time To Live for the cached result:

```scala 
import concurrent.duration._
import language.postfixOps

def getUser(id: Int): User = memoize(60 seconds) {
  // Do DB lookup here...
  User(id, s"user${id}")
}
```

In the above sample, the retrieved User object will be evicted from the cache after 60 seconds.

#### How it works

Like Spring Cache and similar frameworks, ScalaCache automatically builds a cache key based on the method being called, and the values of the arguments being passed to that method.
However, instead of using proxies like Spring, it makes use of Scala macros, so most of the information needed to build the cache key is gathered at compile time. No reflection or AOP magic is required at runtime.

#### Cache key generation

The cache key is built automatically from the class name, the name of the enclosing method, and the values of all of the method's parameters.

For example, given the following method:

```scala 
package foo

object Bar {
  def baz(a: Int, b: String)(c: String): Int = memoize {
    // Reticulating splines...   
    123
  }
}
```

the result of the method call
```scala 
val result = Bar.baz(1, "hello")("world")
```

would be cached with the key: `foo.bar.Baz(1, hello)(world)`.

Note that the cache key generation logic is customizable.

## Cache implementations

### Google Guava

SBT:

```
libraryDependencies += "com.github.cb372" %% "scalacache-guava" % "0.3.2"
```

Usage:

```scala
import scalacache._
import guava._

implicit val scalaCache = ScalaCache(GuavaCache())
```

This will build a Guava cache with all the default settings. If you want to customize your Guava cache, then build it yourself and pass it to `GuavaCache` like this:

```scala
import scalacache._
import guava._
import com.google.common.cache.CacheBuilder

val underlyingGuavaCache = CacheBuilder.newBuilder().maximumSize(10000L).build[String, Object]
implicit val scalaCache = ScalaCache(GuavaCache(underlyingGuavaCache))
```

### Memcached

SBT:

```
libraryDependencies += "com.github.cb372" %% "scalacache-memcached" % "0.3.2"
```

Usage:

```scala
import scalacache._
import memcached._

implicit val scalaCache = ScalaCache(MemcachedCache("host:port"))
```

or provide your own Memcached client, like this:

```scala
import scalacache._
import memcached._
import net.spy.memcached.MemcachedClient

val memcachedClient = new MemcachedClient(...)
implicit val scalaCache = ScalaCache(MemcachedCache(memcachedClient))
```

#### Keys

Memcached only accepts ASCII keys with length <= 250 characters (see the [spec](https://github.com/memcached/memcached/blob/1.4.20/doc/protocol.txt#L41) for more details).

ScalaCache provides two `KeySanitizer` implementations that convert your cache keys into valid Memcached keys.

* `ReplaceAndTruncateSanitizer` simply replaces non-ASCII characters with underscores and truncates long keys to 250 chars. This sanitizer is convenient because it keeps your keys human-readable. Use it if you only expect ASCII characters to appear in cache keys and you don't use any massively long keys.

* `HashingMemcachedKeySanitizer` uses a hash of your cache key, so it can turn any string into a valid Memcached key. The only downside is that it turns your keys into gobbledigook, which can make debugging a pain.

### Ehcache

SBT:

```
libraryDependencies += "com.github.cb372" %% "scalacache-ehcache" % "0.3.2"
```

Usage:

```scala
import scalacache._
import ehcache._

// We assume you've already taken care of Ehcache config, 
// and you have an initialized Ehcache cache.
val cacheManager: net.sf.ehcache.CacheManager = ...
val underlying: net.sf.ehcache.Cache = cacheManager.getCache("myCache")

implicit val scalaCache = ScalaCache(EhcacheCache(underlying))
```

### Redis

SBT:

```
libraryDependencies += "com.github.cb372" %% "scalacache-redis" % "0.3.2"
```

Usage:

```scala
import scalacache._
import redis._

implicit val scalaCache = ScalaCache(RedisCache("host1", 6379))
```

or provide your own [Jedis](https://github.com/xetorthio/jedis) client, like this:

```scala
import scalacache._
import redis._
import redis.clients.jedis._

val jedis = new Jedis(...)
implicit val scalaCache = ScalaCache(RedisCache(jedis))
```

## Troubleshooting/Restrictions

Methods containing `memoize` blocks must have an explicit return type.
If you don't specify the return type, you'll get a confusing compiler error along the lines of `recursive method withExpiry needs result type`.

For example, this is OK

```scala
def getUser(id: Int): User = memoize {
  // Do stuff...
}
```

but this is not

```scala
def getUser(id: Int) = memoize {
  // Do stuff...
}
```
