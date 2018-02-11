---
layout: docs
title: Modes
---

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
libraryDependencies += "com.github.cb372" %% "scalacache-cats-effect" % "0.22.0"
```

```tut:silent
import scalacache.CatsEffect.modes._
```

* Wraps the operation in `IO`, deferring execution until it is explicitly run

#### Monix Task

You will need a dependency on the `scalacache-monix` module:

```
libraryDependencies += "com.github.cb372" %% "scalacache-monix" % "0.22.0"
```

```tut:silent
import scalacache.Monix.modes._
```

* Wraps the operation in `Task`, deferring execution until it is explicitly run

#### Scalaz Task

You will need a dependency on the `scalacache-scalaz72` module:

```
libraryDependencies += "com.github.cb372" %% "scalacache-scalaz72" % "0.22.0"
```

```tut:silent
import scalacache.Scalaz72.modes._
```

* Wraps the operation in `Task`, deferring execution until it is explicitly run
