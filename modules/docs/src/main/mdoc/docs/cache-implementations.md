---
layout: docs
title: Cache implementations
---

## Cache implementations

### Memcached

SBT:

```
libraryDependencies += "com.github.cb372" %% "scalacache-memcached" % "0.28.0"
```

Usage:

```scala mdoc:silent
import scalacache._
import scalacache.memcached._
import scalacache.serialization.binary._
import cats.effect.IO

implicit val memcachedCache: Cache[IO, String, String] = MemcachedCache("localhost:11211")
```

or provide your own Memcached client, like this:

```scala mdoc:silent
import scalacache._
import scalacache.memcached._
import scalacache.serialization.binary._
import net.spy.memcached._

val memcachedClient = new MemcachedClient(
  new BinaryConnectionFactory(), 
  AddrUtil.getAddresses("localhost:11211")
)
implicit val customisedMemcachedCache: Cache[IO, String, String] = MemcachedCache(memcachedClient)
```

#### Keys

Memcached only accepts ASCII keys with length <= 250 characters (see the [spec](https://github.com/memcached/memcached/blob/1.4.20/doc/protocol.txt#L41) for more details).

ScalaCache provides two `KeySanitizer` implementations that convert your cache keys into valid Memcached keys.

* `ReplaceAndTruncateSanitizer` simply replaces non-ASCII characters with underscores and truncates long keys to 250 chars. This sanitizer is convenient because it keeps your keys human-readable. Use it if you only expect ASCII characters to appear in cache keys and you don't use any massively long keys.

* `HashingMemcachedKeySanitizer` uses a hash of your cache key, so it can turn any string into a valid Memcached key. The only downside is that it turns your keys into gobbledigook, which can make debugging a pain. 

### Redis

SBT:

```
libraryDependencies += "com.github.cb372" %% "scalacache-redis" % "0.28.0"
```

Usage:

```scala mdoc:silent
import scalacache._
import scalacache.redis._
import scalacache.serialization.binary._
import cats.effect.IO

implicit val redisCache: Cache[IO, String, String] = RedisCache("host1", 6379)
```

or provide your own [Jedis](https://github.com/xetorthio/jedis) client, like this:

```scala mdoc:silent
import scalacache._
import scalacache.redis._
import scalacache.serialization.binary._
import _root_.redis.clients.jedis._
import cats.effect.IO

val jedisPool = new JedisPool("localhost", 6379)
implicit val customisedRedisCache: Cache[IO, String, String] = RedisCache(jedisPool)
```

ScalaCache also supports [sharded Redis](https://github.com/xetorthio/jedis/wiki/AdvancedUsage#shardedjedis) and [Redis Sentinel](http://redis.io/topics/sentinel). Just create a `ShardedRedisCache` or `SentinelRedisCache` respectively.

### Caffeine

SBT:

```
libraryDependencies += "com.github.cb372" %% "scalacache-caffeine" % "0.28.0"
```

Usage:

```scala mdoc:silent
import scalacache._
import scalacache.caffeine._
import cats.effect.{Clock, IO}
import cats.effect.unsafe.implicits.global

implicit val clock: Clock[IO] = Clock[IO]

implicit val caffeineCache: Cache[IO, String, String] = CaffeineCache[IO, String, String].unsafeRunSync()
```

This will build a Caffeine cache with all the default settings. If you want to customize your Caffeine cache, then build it yourself and pass it to `CaffeineCache` like this:

```scala mdoc:silent
import scalacache._
import scalacache.caffeine._
import com.github.benmanes.caffeine.cache.Caffeine
import cats.effect.IO
import cats.effect.unsafe.implicits.global

val underlyingCaffeineCache = Caffeine.newBuilder().maximumSize(10000L).build[String, Entry[String]]
implicit val customisedCaffeineCache: Cache[IO, String, String] = CaffeineCache(underlyingCaffeineCache)
```

```scala mdoc:invisible
for (cache <- List(redisCache, customisedRedisCache, memcachedCache, customisedMemcachedCache)) {
  cache.close.unsafeRunSync()
} 
```
