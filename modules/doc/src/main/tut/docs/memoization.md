---
layout: docs
title: Memoization
---

### Memoization of method results

```tut
import scalacache._
import scalacache.memcached._
import scalacache.memoization._

import scalacache.serialization.binary._

import scalacache.modes.try_._
import scala.concurrent.duration._
import scala.util.Try

final case class Cat(id: Int, name: String, colour: String)

implicit val catsCache: Cache[Cat] = MemcachedCache("localhost:11211")

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

```tut:invisible
for (cache <- List(catsCache)) {
  cache.close()(scalacache.modes.sync.mode)
} 
```
