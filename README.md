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

## How to use

### Imports

At the very least you will need to import the ScalaCache API.

```scala
import scalacache._
```

Note that this import also brings a bunch of useful implicit magic into scope.

### Create a cache

You'll need to choose a cache implementation. If you want a high performance in-memory cache, Caffeine is a good choice. For a distributed cache, shared between multiple instances of your application, you might want Redis or Memcached.

Let's go with Memcached for this example, assuming that there is a Memcached server running on localhost.

The constructor takes a type parameter, which is the type of the values you want to store in the cache.

```scala
import scalacache.memcached._

case class Cat(id: Int, name: String, colour: String)

implicit val catsCache: Cache[Cat] = MemcachedCache("localhost:11211")
```

Note that we made the cache `implicit` so that the ScalaCache API can find it.

### Basic cache operations

```scala
scala> val ericTheCat = Cat(1, "Eric", "tuxedo")
ericTheCat: Cat = Cat(1,Eric,tuxedo)

scala> val doraemon = Cat(99, "Doraemon", "blue")
doraemon: Cat = Cat(99,Doraemon,blue)

scala> // Choose the Try mode (more on this later)
     | import scalacache.modes.try_._
import scalacache.modes.try_._

scala> // Add an item to the cache
     | put("eric")(ericTheCat)
res4: scala.util.Try[Any] = Success(())

scala> // Add an item to the cache with a Time To Live
     | import scala.concurrent.duration._
import scala.concurrent.duration._

scala> put("doraemon")(doraemon, ttl = Some(10.seconds))
res6: scala.util.Try[Any] = Success(())

scala> // Retrieve the added item
     | get("eric")
res8: scala.util.Try[Option[Cat]] = Success(Some(Cat(1,Eric,tuxedo)))

scala> // Remove it from the cache
     | remove("doraemon")
res10: scala.util.Try[Any] = Success(())

scala> // Flush the cache
     | removeAll[Cat]()
res12: scala.util.Try[Any] = Success(())

scala> // Wrap any block with caching: if the key is not present in the cache,
     | // the block will be executed and the value will be cached and returned
     | val result = caching("benjamin")(ttl = None) {
     |   // e.g. call an external API ...
     |   Cat(2, "Benjamin", "ginger")
     | }
result: scala.util.Try[Cat] = Success(Cat(2,Benjamin,ginger))

scala> // If the result of the block is wrapped in an effect, use cachingF
     | val result = cachingF("benjamin")(ttl = None) {
     | 	import scala.util.Try
     |   Try { 
     |     // e.g. call an external API ...
     |     Cat(2, "Benjamin", "ginger")
     |   }
     | }
result: scala.util.Try[Cat] = Success(Cat(2,Benjamin,ginger))

scala> // You can also pass multiple parts to be combined into one key
     | put("foo", 123, "bar")(ericTheCat) // Will be cached with key "foo:123:bar"
res17: scala.util.Try[Any] = Success(())
```
### Modes

Depending on your application, you might want ScalaCache to wrap its operations in a Try, a Future, a Scalaz Task, or some other effect container.

Or maybe you want to keep it simple and just return plain old values, performing the operations on the current thread and throwing exceptions in case of failure.

In order to control ScalaCache's behaviour in this way, you need to choose a "mode".

ScalaCache comes with a few built-in modes.

#### Synchronous mode

```scala
import scalacache.modes.sync._
```

* Blocks the current thread until the operation completes
* Returns a plain value, not wrapped in any container
* Throws exceptions in case of failure

#### Try mode

```scala
import scalacache.modes.try_._
```

* Blocks the current thread until the operation completes
* Wraps failures in `scala.util.Failure`

#### Future mode

```scala
import scalacache.modes.scalaFuture._
```

* Executes the operation on a separate thread and returns a `scala.util.Future`

You will also need an ExecutionContext in implicit scope:

```scala
import scala.concurrent.ExecutionContext.Implicits.global
```

#### cats-effect IO mode

You will need a dependency on the `scalacache-cats-effect` module:

```
libraryDependencies += "com.github.cb372" %% "scalacache-cats-effect" % "0.10.0"
```

```scala
import scalacache.CatsEffect.modes._
```

* Wraps the operation in `IO`, deferring execution until it is explicitly run

#### Monix Task

You will need a dependency on the `scalacache-monix` module:

```
libraryDependencies += "com.github.cb372" %% "scalacache-monix" % "0.10.0"
```

```scala
import scalacache.Monix.modes._
```

* Wraps the operation in `Task`, deferring execution until it is explicitly run

#### Scalaz Task

You will need a dependency on the `scalacache-scalaz72` module:

```
libraryDependencies += "com.github.cb372" %% "scalacache-scalaz72" % "0.10.0"
```

```scala
import scalacache.Scalaz72.modes._
```

* Wraps the operation in `Task`, deferring execution until it is explicitly run

### Synchronous API

Unfortunately Scala's type inference doesn't play very nicely with ScalaCache's synchronous mode.

If you want to use synchronous mode, the synchronous API is recommended.

```scala
scala> import scalacache._
import scalacache._

scala> import scalacache.modes.sync._
import scalacache.modes.sync._

scala> val myValue: Option[Cat] = sync.get("eric")
myValue: Option[Cat] = None
```

If you're using an in-memory cache (e.g. Guava) then this is fine. But if you're communicating with a cache over a network (e.g. Redis, Memcached) then `sync.get` is not recommended. If the network goes down, your app could hang forever!

There is also a synchronous version of `caching`:

```scala
scala> val result = sync.caching("myKey")(ttl = None) {
     |   // do stuff...
     |   ericTheCat
     | }
result: Cat = Cat(1,Eric,tuxedo)
```

### Memoization of method results


```scala
scala> import scalacache._
import scalacache._

scala> import scalacache.memoization._
import scalacache.memoization._

scala> import scalacache.modes.try_._
import scalacache.modes.try_._

scala> import scala.util.Try
import scala.util.Try

scala> // TODO sort out ambiguous implicit here
     | //def getCat(id: Int): Try[Cat] = memoize(Some(10.seconds)) {
     | //  // Retrieve data from a remote API here ...
     | //  Cat(id, s"cat ${id}", "black")
     | //}
```

Did you spot the magic word 'memoize' in the `getCat` method? Just adding this keyword will cause the result of the method to be memoized to a cache.
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

Note that the cache key generation logic is customizable. Just provide your own implementation of [MethodCallToStringConverter](core/shared/src/main/scala/scalacache/memoization/MethodCallToStringConverter.scala)

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

then you want the cache key to depend on the values of both `a` and `b`. In that case, you need to use a different implementation of [MethodCallToStringConverter](core/shared/src/main/scala/scalacache/memoization/MethodCallToStringConverter.scala), like this:

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

You can set flags by defining a [scalacache.Flags](core/shared/src/main/scala/scalacache/Flags.scala) instance in implicit scope.

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

## Serialization / Deserialization

For cache implementations that do not store their data locally (like [Memcached](#memcached) and [Redis](#redis)), serialization and
deserialization of data to and from `Array[Byte]` is handled by a `Codec` type class. We provide efficient `Codec` instances for all primitive
types, and provide an implementation for objects based on Java serialisation. 

### Custom Codec

If you want to use a custom `Codec` for your object of type `A`, simply implement an instance of `Codec[A]` and make sure it
is in scope at your `get`/`put` call site.

### Compression of `Codec[A]`

If you want to compress your serialised data before sending it to your cache, ScalaCache has a built-in `GZippingBinaryCodec[A]` mix-in
trait that will automatically apply GZip  compression before sending it over the wire if the `Array[Byte]` representation is above a `sizeThreshold`. 
It also takes care of properly decompressing data upon retrieval. To use it, simply extend your `Codec[A]` with `GZippingBinaryCodec[A]` 
**last** (it should be the right-most extended trait).

Those who want to use GZip compression with standard Java serialisation can `import scalacache.serialization.GZippingJavaAnyBinaryCodec._` or
provide an implicit `GZippingJavaAnyBinaryCodec` at the cache call site.

## Cache implementations

### Google Guava

SBT:

```
libraryDependencies += "com.github.cb372" %% "scalacache-guava" % "0.10.0"
```

Usage:

```scala
import scalacache._
import scalacache.guava._

implicit val guavaCache: Cache[String] = GuavaCache[String]
```

This will build a Guava cache with all the default settings. If you want to customize your Guava cache, then build it yourself and pass it to `GuavaCache` like this:

```scala
import scalacache._
import scalacache.guava._
import com.google.common.cache.CacheBuilder

val underlyingGuavaCache = CacheBuilder.newBuilder().maximumSize(10000L).build[String, Entry[String]]
implicit val guavaCache: Cache[String] = GuavaCache(underlyingGuavaCache)
```

### Memcached

SBT:

```
libraryDependencies += "com.github.cb372" %% "scalacache-memcached" % "0.10.0"
```

Usage:

```scala
import scalacache._
import scalacache.memcached._

implicit val memcachedCache: Cache[String] = MemcachedCache("localhost:11211")
```

or provide your own Memcached client, like this:

```scala
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
libraryDependencies += "com.github.cb372" %% "scalacache-ehcache" % "0.10.0"
```

Usage:

```scala
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
libraryDependencies += "com.github.cb372" %% "scalacache-redis" % "0.10.0"
```

Usage:

```scala
import scalacache._
import scalacache.redis._

implicit val redisCache: Cache[String] = RedisCache("host1", 6379)
```

or provide your own [Jedis](https://github.com/xetorthio/jedis) client, like this:

```scala
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
libraryDependencies += "com.github.cb372" %% "scalacache-caffeine" % "0.10.0"
```

Usage:

```scala
import scalacache._
import scalacache.caffeine._

implicit val caffeineCache: Cache[String] = CaffeineCache[String]
```

This will build a Caffeine cache with all the default settings. If you want to customize your Caffeine cache, then build it yourself and pass it to `CaffeineCache` like this:

```scala
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



