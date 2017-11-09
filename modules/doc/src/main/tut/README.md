# ScalaCache

[![Join the chat at https://gitter.im/cb372/scalacache](https://badges.gitter.im/cb372/scalacache.svg)](https://gitter.im/cb372/scalacache?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/cb372/scalacache.png?branch=master)](https://travis-ci.org/cb372/scalacache) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.cb372/scalacache-core_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.cb372/scalacache-core_2.11)

A facade for the most popular cache implementations, with a simple, idiomatic Scala API.

Use ScalaCache to add caching to any Scala app with the minimum of fuss.

The following cache implementations are supported, and it's easy to plugin your own implementation:
* Google Guava
* Memcached
* Ehcache
* Redis
* [Caffeine](https://github.com/ben-manes/caffeine)

## Compatibility

ScalaCache is available for Scala 2.11.x and 2.12.x.

The JVM must be Java 8 or newer.

## Getting Started

### Imports

At the very least you will need to import the ScalaCache API.

```tut:silent
import scalacache._
```

Note that this import also brings a bunch of useful implicit magic into scope.

### Create a cache

You'll need to choose a cache implementation. If you want a high performance in-memory cache, Caffeine is a good choice. For a distributed cache, shared between multiple instances of your application, you might want Redis or Memcached.

Let's go with Memcached for this example, assuming that there is a Memcached server running on localhost.

The constructor takes a type parameter, which is the type of the values you want to store in the cache.

```tut:silent
import scalacache.memcached._

// We'll use the binary serialization codec - more on that later
import scalacache.serialization.binary._

case class Cat(id: Int, name: String, colour: String)

implicit val catsCache: Cache[Cat] = MemcachedCache("localhost:11211")
```

Note that we made the cache `implicit` so that the ScalaCache API can find it.

### Basic cache operations

```tut
val ericTheCat = Cat(1, "Eric", "tuxedo")
val doraemon = Cat(99, "Doraemon", "blue")

// Choose the Try mode (more on this later)
import scalacache.modes.try_._

// Add an item to the cache
put("eric")(ericTheCat)

// Add an item to the cache with a Time To Live
import scala.concurrent.duration._
put("doraemon")(doraemon, ttl = Some(10.seconds))

// Retrieve the added item
get("eric")

// Remove it from the cache
remove("doraemon")

// Flush the cache
removeAll[Cat]()

// Wrap any block with caching: if the key is not present in the cache,
// the block will be executed and the value will be cached and returned
val result = caching("benjamin")(ttl = None) {
  // e.g. call an external API ...
  Cat(2, "Benjamin", "ginger")
}

// If the result of the block is wrapped in an effect, use cachingF
val result = cachingF("benjamin")(ttl = None) {
	import scala.util.Try
  Try { 
    // e.g. call an external API ...
    Cat(2, "Benjamin", "ginger")
  }
}

// You can also pass multiple parts to be combined into one key
put("foo", 123, "bar")(ericTheCat) // Will be cached with key "foo:123:bar"
```
### Modes

Depending on your application, you might want ScalaCache to wrap its operations in a Try, a Future, a Scalaz Task, or some other effect container.

Or maybe you want to keep it simple and just return plain old values, performing the operations on the current thread and throwing exceptions in case of failure.

In order to control ScalaCache's behaviour in this way, you need to choose a "mode".

ScalaCache comes with a few built-in modes.

#### Synchronous mode

```tut:silent
import scalacache.modes.sync._
```

* Blocks the current thread until the operation completes
* Returns a plain value, not wrapped in any container
* Throws exceptions in case of failure

Note: If you're using an in-memory cache (e.g. Guava or Caffeine) then it makes sense to use the synchronous mode. But if you're communicating with a cache over a network (e.g. Redis, Memcached) then this mode is not recommended. If the network goes down, your app could hang forever!

#### Try mode

```tut:silent
import scalacache.modes.try_._
```

* Blocks the current thread until the operation completes
* Wraps failures in `scala.util.Failure`

#### Future mode

```tut:silent
import scalacache.modes.scalaFuture._
```

* Executes the operation on a separate thread and returns a `scala.util.Future`

You will also need an ExecutionContext in implicit scope:

```tut:silent
import scala.concurrent.ExecutionContext.Implicits.global
```

#### cats-effect IO mode

You will need a dependency on the `scalacache-cats-effect` module:

```
libraryDependencies += "com.github.cb372" %% "scalacache-cats-effect" % "0.20.0"
```

```tut:silent
import scalacache.CatsEffect.modes._
```

* Wraps the operation in `IO`, deferring execution until it is explicitly run

#### Monix Task

You will need a dependency on the `scalacache-monix` module:

```
libraryDependencies += "com.github.cb372" %% "scalacache-monix" % "0.20.0"
```

```tut:silent
import scalacache.Monix.modes._
```

* Wraps the operation in `Task`, deferring execution until it is explicitly run

#### Scalaz Task

You will need a dependency on the `scalacache-scalaz72` module:

```
libraryDependencies += "com.github.cb372" %% "scalacache-scalaz72" % "0.20.0"
```

```tut:silent
import scalacache.Scalaz72.modes._
```

* Wraps the operation in `Task`, deferring execution until it is explicitly run

### Synchronous API

Unfortunately Scala's type inference doesn't play very nicely with ScalaCache's synchronous mode.

If you want to use synchronous mode, the synchronous API is recommended. This works in exactly the same way as the normal API; it is provided merely as a convenience so you don't have to jump through hoops to make your code compile when using the synchronous mode.

```tut
import scalacache._
import scalacache.modes.sync._

val myValue: Option[Cat] = sync.get("eric")
```

There is also a synchronous version of `caching`:

```tut
val result = sync.caching("myKey")(ttl = None) {
  // do stuff...
  ericTheCat
}
```

### Memoization of method results

```tut
import scalacache.memoization._

import scalacache.modes.try_._
import scala.util.Try

// You wouldn't normally need to specify the type params for memoize.
// This is an artifact of the way this README is generated using tut.
def getCat(id: Int): Try[Cat] = memoize[Try, Cat](Some(10.seconds)) {
  // Retrieve data from a remote API here ...
  Cat(id, s"cat ${id}", "black")
}

getCat(123)
```

Did you spot the magic word 'memoize' in the `getCat` method? Just adding this keyword will cause the result of the method to be memoized to a cache.
The next time you call the method with the same arguments the result will be retrieved from the cache and returned immediately.

If the result of your block is wrapped in an effect container, use `memoizeF`:

```tut
def getCatF(id: Int): Try[Cat] = memoizeF[Try, Cat](Some(10.seconds)) {
  Try {
    // Retrieve data from a remote API here ...
    Cat(id, s"cat ${id}", "black")
  }
}

getCatF(123)
```

#### Synchronous memoization API

Again, there is a synchronous memoization method for convient use of the synchronous mode:

```tut 
import scalacache.modes.sync._

def getCatSync(id: Int): Cat = memoizeSync(Some(10.seconds)) {
  // Do DB lookup here ...
  Cat(id, s"cat ${id}", "black")
}

getCatSync(123)
```

#### How it works

`memoize` automatically builds a cache key based on the method being called, and the values of the arguments being passed to that method.

Under the hood it makes use of Scala macros, so most of the information needed to build the cache key is gathered at compile time. No reflection or AOP magic is required at runtime.

#### Cache key generation

The cache key is built automatically from the class name, the name of the enclosing method, and the values of all of the method's parameters.

For example, given the following method:

```scala
package foo

object Bar {
  def baz(a: Int, b: String)(c: String): Int = memoizeSync(None) {
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

Note that the cache key generation logic is customizable. Just provide your own implementation of [MethodCallToStringConverter](core/shared/src/main/scala/scalacache/memoization/MethodCallToStringConverter.scala)

#### Enclosing class's constructor arguments

If your memoized method is inside a class, rather than an object, then the method's result might depend on values passed to that class's constructor.

For example, if your code looks like this:

```scala 
package foo

class Bar(a: Int) {

  def baz(b: Int): Int = memoizeSync(None) {
    a + b
  }
  
}
```

then you want the cache key to depend on the values of both `a` and `b`. In that case, you need to use a different implementation of [MethodCallToStringConverter](core/shared/src/main/scala/scalacache/memoization/MethodCallToStringConverter.scala), like this:

```tut:silent
implicit val cacheConfig = CacheConfig(
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
def doSomething(userId: UserId)(implicit @cacheKeyExclude db: DBConnection): String = memoize {
  ...
}
```

will only include the `userId` argument's value in its cache keys.

### Flags

Cache GETs and/or PUTs can be temporarily disabled using flags. This can be useful if for example you want to skip the cache and read a value from the DB under certain conditions.

You can set flags by defining a [scalacache.Flags](core/shared/src/main/scala/scalacache/Flags.scala) instance in implicit scope.

The detailed behaviour of the flags is as follows:

* If `readsEnabled` = false, the cache will not be read, and ScalaCache will behave as if it was a cache miss. This means that memoization will compute the value (e.g. read it from a DB) and then write it to the cache.
* If `writesEnabled` = false, in the case of a cache miss, the value will be computed (e.g. read from a DB) but it will not be written to the cache.
* If both flags are false, memoization will not read from the cache or write to the cache.

Note that your memoized method must take an implicit parameter of type `Flags`. Otherwise any flags you try to set using an implicit will be silently ignored.

Example:

```tut:silent
def getCatWithFlags(id: Int)(implicit flags: Flags): Cat = memoizeSync(None) {
  // Do DB lookup here...
  Cat(id, s"cat ${id}", "black")
}

def getCatMaybeSkippingCache(id: Int, skipCache: Boolean): Cat = {
  implicit val flags = Flags(readsEnabled = !skipCache)
  getCatWithFlags(id)
}
```

Tip: Because the flags are passed as a parameter to your method, they will be included in the generated cache key. This means the cache key will vary depending on the value of the flags, which is probably not what you want. In that case, you should exclude the `implicit flags: Flags` parameter from cache key generation by annotating it with `@cacheKeyExclude`.

## Serialization

If you are using a cache implementation that does not store its data locally (like [Memcached](#memcached) and [Redis](#redis)), you will need to choose a codec in order to serialize your data to bytes.

### Binary codec

ScalaCache provides efficient `Codec` instances for all primitive types, and also an implementation for objects based on Java serialization.

To use this codec, you need one import:

```tut:silent
import scalacache.serialization.binary._
```

### JSON codec

If you want to serialize your values as JSON, you can use ScalaCache's [circe](https://circe.github.io/circe/) integration.

You will need to add a dependency on the scalacache-circe module:

```
libraryDependencies += "com.github.cb372" %% "scalacache-circe" % "0.20.0"
```

Then import the codec:

```tut:silent
import scalacache.serialization.circe._
```

If your cache holds values of type `Cat`, you will also need a Circe `Encoder[Cat]` and `Decoder[Cat]` in implicit scope. The easiest way to do this is to ask circe to automatically derive them:

```scala
import io.circe.generic.auto._
```

but if you are worried about performance, it's better to derive them semi-automatically:

```scala
import io.circe._
import io.circe.generic.semiauto._
implicit val catEncoder: Encoder[Cat] = deriveEncoder[Cat]
implicit val catDecoder: Decoder[Cat] = deriveDecoder[Cat]
```

For more information, please consult the [circe docs](https://circe.github.io/circe/).

### Custom Codec

If you want to use a custom `Codec` for your object of type `A`, simply implement an instance of `Codec[A]` and make sure it
is in scope at your `get`/`put` call site.

### Compression of `Codec[A]`

If you want to compress your serialized data before sending it to your cache, ScalaCache has a built-in `GZippingBinaryCodec[A]` mix-in
trait that can be used to decorate another codec. It will automatically apply GZip compression to the encoded value if the `Array[Byte]` representation is above a `sizeThreshold`. It also takes care of properly decompressing data upon retrieval.

To use it, simply extend your `Codec[A]` with `GZippingBinaryCodec[A]` **last** (it should be the right-most extended trait).

If you want to use GZip compression with the standard ScalaCache binary codec can either `import scalacache.serialization.gzip.GZippingJavaSerializationCodec._` or provide an implicit `GZippingJavaAnyBinaryCodec` at the cache call site.

## Cache implementations

### Google Guava

SBT:

```
libraryDependencies += "com.github.cb372" %% "scalacache-guava" % "0.20.0"
```

Usage:

```tut:silent
import scalacache._
import scalacache.guava._

implicit val guavaCache: Cache[String] = GuavaCache[String]
```

This will build a Guava cache with all the default settings. If you want to customize your Guava cache, then build it yourself and pass it to `GuavaCache` like this:

```tut:silent
import scalacache._
import scalacache.guava._
import com.google.common.cache.CacheBuilder

val underlyingGuavaCache = CacheBuilder.newBuilder().maximumSize(10000L).build[String, Entry[String]]
implicit val guavaCache: Cache[String] = GuavaCache(underlyingGuavaCache)
```

### Memcached

SBT:

```
libraryDependencies += "com.github.cb372" %% "scalacache-memcached" % "0.20.0"
```

Usage:

```tut:silent
import scalacache._
import scalacache.memcached._

implicit val memcachedCache: Cache[String] = MemcachedCache("localhost:11211")
```

or provide your own Memcached client, like this:

```tut:silent
import scalacache._
import scalacache.memcached._
import net.spy.memcached._

val memcachedClient = new MemcachedClient(
  new BinaryConnectionFactory(), 
  AddrUtil.getAddresses("localhost:11211")
)
implicit val customisedMemcachedCache: Cache[String] = MemcachedCache(memcachedClient)
```

#### Keys

Memcached only accepts ASCII keys with length <= 250 characters (see the [spec](https://github.com/memcached/memcached/blob/1.4.20/doc/protocol.txt#L41) for more details).

ScalaCache provides two `KeySanitizer` implementations that convert your cache keys into valid Memcached keys.

* `ReplaceAndTruncateSanitizer` simply replaces non-ASCII characters with underscores and truncates long keys to 250 chars. This sanitizer is convenient because it keeps your keys human-readable. Use it if you only expect ASCII characters to appear in cache keys and you don't use any massively long keys.

* `HashingMemcachedKeySanitizer` uses a hash of your cache key, so it can turn any string into a valid Memcached key. The only downside is that it turns your keys into gobbledigook, which can make debugging a pain. 

### Ehcache

SBT:

```
libraryDependencies += "com.github.cb372" %% "scalacache-ehcache" % "0.20.0"
```

Usage:

```tut:silent
import scalacache._
import scalacache.ehcache._
import net.sf.ehcache.{Cache => UnderlyingCache, _}

// We assume you've already taken care of Ehcache config, 
// and you have an initialized Ehcache cache.
val cacheManager = new CacheManager
val underlying: UnderlyingCache = cacheManager.getCache("myCache")

implicit val ehcacheCache: Cache[String] = EhcacheCache(underlying)
```

### Redis

SBT:

```
libraryDependencies += "com.github.cb372" %% "scalacache-redis" % "0.20.0"
```

Usage:

```tut:silent
import scalacache._
import scalacache.redis._

implicit val redisCache: Cache[String] = RedisCache("host1", 6379)
```

or provide your own [Jedis](https://github.com/xetorthio/jedis) client, like this:

```tut:silent
import scalacache._
import scalacache.redis._
import _root_.redis.clients.jedis._

val jedisPool = new JedisPool("localhost", 6379)
implicit val customisedRedisCache: Cache[String] = RedisCache(jedisPool)
```

ScalaCache also supports [sharded Redis](https://github.com/xetorthio/jedis/wiki/AdvancedUsage#shardedjedis) and [Redis Sentinel](http://redis.io/topics/sentinel). Just create a `ShardedRedisCache` or `SentinelRedisCache` respectively.

## Caffeine

SBT:

```
libraryDependencies += "com.github.cb372" %% "scalacache-caffeine" % "0.20.0"
```

Usage:

```tut:silent
import scalacache._
import scalacache.caffeine._

implicit val caffeineCache: Cache[String] = CaffeineCache[String]
```

This will build a Caffeine cache with all the default settings. If you want to customize your Caffeine cache, then build it yourself and pass it to `CaffeineCache` like this:

```tut:silent
import scalacache._
import scalacache.caffeine._
import com.github.benmanes.caffeine.cache.Caffeine

val underlyingCaffeineCache = Caffeine.newBuilder().maximumSize(10000L).build[String, Entry[String]]
implicit val customisedCaffeineCache: Cache[String] = CaffeineCache(underlyingCaffeineCache)
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

## About this README

This README is generated using [tut](https://github.com/tpolecat/tut), so all the code samples are known to compile and run.

To make a change to the README:

1. Make sure that memcached is running on localhost:11211
2. Edit [modules/doc/src/main/tut/README.md](modules/doc/src/main/tut/README.md)
3. Run `sbt doc/tut`
4. Commit both the source file `modules/doc/src/main/tut/README.md` and the generated file `./README.md` to git.

```tut:invisible
for (cache <- List(catsCache, ehcacheCache, redisCache, customisedRedisCache, memcachedCache, customisedMemcachedCache)) {
  cache.close()(scalacache.modes.sync.mode)
} 
```
