# ScalaCache

[![Join the chat at https://gitter.im/cb372/scalacache](https://badges.gitter.im/cb372/scalacache.svg)](https://gitter.im/cb372/scalacache?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/cb372/scalacache.png?branch=master)](https://travis-ci.org/cb372/scalacache) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.cb372/scalacache-core_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.cb372/scalacache-core_2.11)

(formerly known as Cacheable)

A facade for the most popular cache implementations, with a simple, idiomatic Scala API.

Use ScalaCache to add caching to any Scala app with the minimum of fuss.

The following cache implementations are supported, and it's easy to plugin your own implementation:
* Google Guava
* Memcached
* Ehcache
* Redis
* [Caffeine](https://github.com/ben-manes/caffeine)

## Versioning

ScalaCache is available for Scala 2.11.x and 2.12.x.

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
put("myKey")("myValue") // returns a Future[Unit]

// Add an item to the cache with a Time To Live
put("otherKey")("otherValue", ttl = Some(10.seconds))

// Retrieve the added item
get[String, NoSerialization]("myKey") // returns a Future of an Option

// If you are using a serializing cache implementation (Memcached or Redis), use Array[Byte]
get[String, Array[Byte]]("myKey") // returns a Future of an Option

// Remove it from the cache
remove("myKey") // returns a Future[Unit]

// Flush the cache
removeAll() // returns a Future[Unit]

// Wrap any block with caching
val future: Future[String] = caching("myKey") {
  Future { 
    // e.g. call an external API ...
    "result of block" 
  }
}

// You can specify a Time To Live if you like
val future: Future[String] = cachingWithTTL("myKey")(10.seconds) {
  Future {
    // do stuff...
    "result of block"
  }
}

// You can also pass multiple parts to be combined into one key
put("foo", 123, "bar")(value) // Will be cached with key "foo:123:bar"
```

### Synchronous API

If you don't want to bother with Futures, you can do a blocking read from the cache using the `getSync` method. This just wraps the `get` method, blocking indefinitely.

```scala
import scalacache._

val myValue: Option[String] = sync.get("myKey")
```

If you're using an in-memory cache (e.g. Guava) then this is fine. But if you're communicating with a cache over a network (e.g. Redis, Memcached) then `sync.get` is not recommended. If the network goes down, your app could hang forever!

There are also synchronous versions of the `caching` and `cachingWithTTL` methods available:

```scala
val result = sync.caching("myKey") {
  // do stuff...
  "result of block"
}

val result = sync.cachingWithTTL("myKey")(10.seconds) {
  // do stuff...
  "result of block"
}
```

### Memoization of method results

```scala 
import scalacache._
import memoization._

implicit val scalaCache = ScalaCache(new MyCache())

def getUser(id: Int): Future[User] = memoize {
  Future {
    // Retrieve data from a remote API here ...
    User(id, s"user${id}")
  }
}
```

Did you spot the magic word 'memoize' in the `getUser` method? Just adding this keyword will cause the result of the method to be memoized to a cache.
The next time you call the method with the same arguments the result will be retrieved from the cache and returned immediately.

#### Time To Live

You can optionally specify a Time To Live for the cached result:

```scala 
import concurrent.duration._
import language.postfixOps

def getUser(id: Int): Future[User] = memoize(60 seconds) {
  Future {
    // Retrieve data from a remote API here ...
    User(id, s"user${id}")
  }
}
```

In the above sample, the retrieved User object will be evicted from the cache after 60 seconds.

#### Synchronous memoization API

Again, there are synchronous equivalents available for the case where you don't want to bother with Futures:

```scala 
import scalacache._
import memoization._

implicit val scalaCache = ScalaCache(new MyCache())

def getUser(id: Int): User = memoizeSync {
  // Do DB lookup here...
  User(id, s"user${id}")
}
```

#### How it works

Like Spring Cache and similar frameworks, ScalaCache automatically builds a cache key based on the method being called, and the values of the arguments being passed to that method.
However, instead of using proxies like Spring, it makes use of Scala macros, so most of the information needed to build the cache key is gathered at compile time. No reflection or AOP magic is required at runtime.

#### Cache key generation

The cache key is built automatically from the class name, the name of the enclosing method, and the values of all of the method's parameters.

For example, given the following method:

```scala 
package foo

object Bar {
  def baz(a: Int, b: String)(c: String): Int = memoizeSync {
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

Note that the cache key generation logic is customizable. Just provide your own implementation of [MethodCallToStringConverter](core/src/main/scala/scalacache/memoization/MethodCallToStringConverter.scala)

#### Enclosing class's constructor arguments

If your memoized method is inside a class, rather than an object, then the method's result might depend on values passed to that class's constructor.

For example, if your code looks like this:

```scala 
package foo

class Bar(a: Int) {

  def baz(b: Int): Int = memoizeSync {
    a + b
  }
  
}
```

then you want the cache key to depend on the values of both `a` and `b`. In that case, you need to use a different implementation of [MethodCallToStringConverter](core/src/main/scala/scalacache/memoization/MethodCallToStringConverter.scala), like this:

```scala 
implicit val scalaCache = ScalaCache(
  cache = ... ,
  memoization = MemoizationConfig(MethodCallToStringConverter.includeClassConstructorParams)
)
```

Doing this will ensure that both the constructor arguments and the method arguments are included in the cache key:

```scala 
new Bar(10).baz(42) // cached as "foo.Bar(10).baz(42)" -> 52
new Bar(20).baz(42) // cached as "foo.Bar(20).baz(42)" -> 62
```

#### Excluding parameters from the generated cache key

If there are any parameters (either method arguments or class constructor arguments) that you don't want to include in the auto-generated cache key for memoization, you can exclude them using the `@cacheKeyExclude` annotation.

For example:

```scala
def doSomething(userId: UserId)(implicit @cacheKeyExclude db: DBConnection) = memoize {
  ...
}
```

will only include the `userId` argument's value in its cache keys.

### Flags

Cache GETs and/or PUTs can be temporarily disabled using flags. This can be useful if for example you want to skip the cache and read a value from the DB under certain conditions.

You can set flags by defining a [scalacache.Flags](core/src/main/scala/scalacache/Flags.scala) instance in implicit scope.

The detailed behaviour of the flags is as follows:

* If `readsEnabled` = false, the cache will not be read, and ScalaCache will behave as if it was a cache miss. This means that memoization will compute the value (e.g. read it from a DB) and then write it to the cache.
* If `writesEnabled` = false, in the case of a cache miss, the value will be computed (e.g. read from a DB) but it will not be written to the cache.
* If both flags are false, memoization will not read from the cache or write to the cache.

Note that your memoized method must take an implicit parameter of type `Flags`. Otherwise any flags you try to set using an implicit will be silently ignored.

Example:

```scala 
import scalacache._
import memoization._

implicit val scalaCache = ScalaCache(new MyCache())

def getUser(id: Int)(implicit flags: Flags): User = memoizeSync {
  // Do DB lookup here...
  User(id, s"user${id}")
}

def getUser(id: Int, skipCache: Boolean): User = {
  implicit val flags = Flags(readsEnabled = !skipCache)
  getUser(id)
}
```

Tip: Because the flags are passed as a parameter to your method, they will be included in the generated cache key. This means the cache key will vary depending on the value of the flags, which is probably not what you want. In that case, you should exclude the `implicit flags: Flags` parameter from cache key generation by annotating it with `@cacheKeyExclude`.

### Typed API

If you are only storing one type of object in your cache, and you want to ensure you don't accidentally cache something of the wrong type, you can use the Typed API:

```scala
import scalacache._

implicit val scalaCache = ScalaCache(new MyCache())

// Use NoSerialization for in-memory caches such as Caffeine, 
// or Array[Byte] for serializing caches such as Memcached or Redis
val cache = typed[User, NoSerialization]

cache.put("key", User(123, "Chris")) // OK
cache.put("key", "wibble") // Compile error!

cache.get("key") // returns Future[Option[User]]
```

## Serialization / Deserialization

For cache implementations that do not store their data locally (like [Memcached](#Memcached) and [Redis](#Redis)), serialization and
deserialization of data to and from `Array[Byte]` is handled by a `Codec` type class. We provide efficient `Codec` instances for all primitive
types, and provide an implementation for objects based on Java serialisation. 

### Custom Codec

If you want to use a custom `Codec` for your object of type `A`, simply implement an instance of `Codec[A, Array[Byte]]` and make sure it
is in scope at your `set`/`put` call site.

### Compression of `Codec[A, Array[Byte]]`

If you want to compress your serialised data before sending it to your cache, ScalaCache has a built-in `GZippingBinaryCodec[A]` mix-in
trait that will automatically apply GZip  compression before sending it over the wire if the `Array[Byte]` representation is above a `sizeThreshold`. 
It also takes care of properly decompressing data upon retrieval. To use it, simply extend your `Codec[A, Array[Byte] with GZippingBinaryCodec[A]` 
**last** (it should be the right-most extended trait).

Those who want to use GZip compression with standard Java serialisation can `import scalacache.serialization.GZippingJavaAnyBinaryCodec._` or
provide an implicit `GZippingJavaAnyBinaryCodec` at the cache call site.

### Backwards compatibility

In the interests of keeping backward compatibility with older versions of ScalaCache before serialization was handled by `Codec`,
`MemcachedCache`, `RedisCache`, `SentinelRedisCache`, and `SharedRedisCache` have a `useLegacySerialization` boolean parameter,
which allows you to continue using the original ScalaCache serialization/deserialization logic.

Legacy users who want to avoid deserialization errors when uprading should set this parameter to `true` *or* clear your caches
before deploying code that depends on ScalaCache after version `0.7.5`

## Cache implementations

### Google Guava

SBT:

```
libraryDependencies += "com.github.cb372" %% "scalacache-guava" % "0.9.3"
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
libraryDependencies += "com.github.cb372" %% "scalacache-memcached" % "0.9.3"
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
libraryDependencies += "com.github.cb372" %% "scalacache-ehcache" % "0.9.3"
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
libraryDependencies += "com.github.cb372" %% "scalacache-redis" % "0.9.3"
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

ScalaCache also supports [sharded Redis](https://github.com/xetorthio/jedis/wiki/AdvancedUsage#shardedjedis) and [Redis Sentinel](http://redis.io/topics/sentinel). Just create a `ShardedRedisCache` or `SentinelRedisCache` respectively.

## Caffeine

Note that Caffeine requires Java 8 or newer.

SBT:

```
libraryDependencies += "com.github.cb372" %% "scalacache-caffeine" % "0.9.3"
```

Usage:

```scala
import scalacache._
import caffeine._

implicit val scalaCache = ScalaCache(CaffeineCache())
```

This will build a Caffeine cache with all the default settings. If you want to customize your Caffeine cache, then build it yourself and pass it to `CaffeineCache` like this:

```scala
import scalacache._
import caffeine._
import com.github.benmanes.caffeine.cache.Caffeine

val underlyingCaffeineCache = Caffeine.newBuilder().maximumSize(10000L).build[String, Object]
implicit val scalaCache = ScalaCache(CaffeineCache(underlyingCaffeineCache))
```

## Troubleshooting/Restrictions

Methods containing `memoize` blocks must have an explicit return type.
If you don't specify the return type, you'll get a confusing compiler error along the lines of `recursive method withExpiry needs result type`.

For example, this is OK

```scala
def getUser(id: Int): Future[User] = memoize {
  // Do stuff...
}
```

but this is not

```scala
def getUser(id: Int) = memoize {
  // Do stuff...
}
```
