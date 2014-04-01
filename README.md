# Cacheable

[![Build Status](https://travis-ci.org/cb372/cacheable.png)](https://travis-ci.org/cb372/cacheable)

A simple and handy library for adding caching to any Scala app with the minimum of fuss.

Cacheable is ideal for caching DB lookups, API calls, or anything else that takes a long time to compute.

The following cache implementations are supported, and it's super-easy to plugin your own implementation:
* Google Guava
* Memcached
* Ehcache
* Redis

## Versioning

Because of the use of Scala macros, only specific Scala versions are supported:

<table>
  <tr><th>Cacheable</th><th>Scala</th></tr>
  <tr><td>0.1.x</td><td>2.10.3</td></tr>
  <tr><td>0.2.x</td><td>2.11.0</td></tr>
</table>

## How to use

```scala 
import cacheable._

// Configuration: the cache implementation to use, and how to generate cache keys
implicit val cacheConfig = CacheConfig(new MyCache(), KeyGenerator.defaultGenerator)

def getUser(id: Int): User = cacheable { 
  // Do DB lookup here...
  User(id, s"user${id}")
}
```

Did you spot the magic word 'cacheable' in the `getUser` method? Just adding this keyword will cause the result of the method to be memoized to a cache, so the next time you call the method the result will be retrieved from the cache.

### Time To Live 

You can optionally specify a Time To Live for the cached result:

```scala 
import concurrent.duration._
import language.postfixOps

def getUser(id: Int): User = cacheable(60 seconds) { 
  // Do DB lookup here...
  User(id, s"user${id}")
}
```

In the above sample, the retrieved User object will be evicted from the cache after 60 seconds.

## How it works

Like Spring Cache and similar frameworks, Cacheable automatically builds a cache key based on the method being called. However, it does *not* use AOP. Instead it makes use of Scala macros, so most of the information needed to build the cache key is gathered at compile time. No reflection or AOP magic at runtime.

### Cache key generation

The cache key is built automatically from the class name, the name of the enclosing method, and the values of all of the method's parameters.

For example, given the following method:

```scala 
package foo

object Bar {
  def baz(a: Int, b: String)(c: String): Int = cacheable { 
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
libraryDependencies += "com.github.cb372" %% "cacheable-guava" % "0.1.1"
```

Usage:

```scala
import cacheable._
import guava._

implicit val cacheConfig = CacheConfig(GuavaCache())
```

This will build a Guava cache with all the default settings. If you want to customize your Guava cache, then build it yourself and pass it to `GuavaCache` like this:

```scala
import cacheable._
import guava._
import com.google.common.cache.CacheBuilder

val underlyingGuavaCache = CacheBuilder.newBuilder().maximumSize(10000L).build[String, Object]
implicit val cacheConfig = CacheConfig(GuavaCache(underlyingGuavaCache))
```

### Memcached

SBT:

```
libraryDependencies += "com.github.cb372" %% "cacheable-memcached" % "0.1.1"
```

Usage:

```scala
import cacheable._
import memcached._

implicit val cacheConfig = CacheConfig(MemcachedCache("host:port"))
```

or provide your own Memcached client, like this:

```scala
import cacheable._
import memcached._
import net.spy.memcached.MemcachedClient

val memcachedClient = new MemcachedClient(...)
implicit val cacheConfig = CacheConfig(MemcachedCache(memcachedClient))
```

### Ehcache

SBT:

```
libraryDependencies += "com.github.cb372" %% "cacheable-ehcache" % "0.1.1"
```

Usage:

```scala
import cacheable._
import ehcache._

// We assume you've already taken care of Ehcache config, 
// and you have an initialized Ehcache cache.
val cacheManager: net.sf.ehcache.CacheManager = ...
val underlying: net.sf.ehcache.Cache = cacheManager.getCache("myCache")

implicit val cacheConfig = CacheConfig(EhcacheCache(underlying))
```

### Redis

SBT:

```
libraryDependencies += "com.github.cb372" %% "cacheable-redis" % "0.1.1"
```

Usage:

```scala
import cacheable._
import redis._

implicit val cacheConfig = CacheConfig(RedisCache("host1", 6379))
```

or provide your own Redis client, like this:

```scala
import cacheable._
import redis._
import com.redis.RedisClient

val redisClient = new RedisClient(...)
implicit val cacheConfig = CacheConfig(RedisCache(redisClient))
```

## Troubleshooting/Restrictions

Methods containing cacheable blocks must have an explicit return type.
If you don't specify the return type, you'll get a confusing compiler error along the lines of `recursive method withExpiry needs result type`.

For example, this is OK

```scala
def getUser(id: Int): User = cacheable {
  // Do stuff...
}
```

but this is not

```scala
def getUser(id: Int) = cacheable {
  // Do stuff...
}
```
