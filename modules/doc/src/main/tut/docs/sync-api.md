---
layout: docs
title: Synchronous API
---

### Synchronous API

Unfortunately Scala's type inference doesn't play very nicely with ScalaCache's synchronous mode.

If you want to use synchronous mode, the synchronous API is recommended. This works in exactly the same way as the normal API; it is provided merely as a convenience so you don't have to jump through hoops to make your code compile when using the synchronous mode.

```tut
import scalacache._
import scalacache.memcached._
import scalacache.modes.sync._

import scalacache.serialization.binary._

final case class Cat(id: Int, name: String, colour: String)

implicit val catsCache: Cache[Cat] = MemcachedCache("localhost:11211")

val myValue: Option[Cat] = sync.get("eric")
val ericTheCat = Cat(1, "Eric", "tuxedo")
```

There is also a synchronous version of `caching`:

```tut
val result = sync.caching("myKey")(ttl = None) {
  // do stuff...
  ericTheCat
}
```

```tut:invisible
for (cache <- List(catsCache)) {
  cache.close()(scalacache.modes.sync.mode)
} 
```
