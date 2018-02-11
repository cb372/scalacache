---
layout: docs
title: Getting started
---

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

final case class Cat(id: Int, name: String, colour: String)

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

```tut:invisible
for (cache <- List(catsCache)) {
  cache.close()(scalacache.modes.sync.mode)
} 
```
