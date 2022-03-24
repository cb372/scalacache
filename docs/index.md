# ScalaCache

[![Build Status](https://github.com/cb372/scalacache/workflows/Continuous%20Integration/badge.svg)](https://github.com/cb372/scalacache/actions) [![Maven Central](https://img.shields.io/maven-central/v/com.github.cb372/scalacache-core_2.13.svg)](http://search.maven.org/#search%7Cga%7C1%7Cscalacache)

A facade for the most popular cache implementations, with a simple, idiomatic Scala API.

Use ScalaCache to add caching to any Scala app with the minimum of fuss.

The following cache implementations are supported, and it's easy to plugin your own implementation:

* [Memcached](https://memcached.org/)
* [Redis](https://redis.io/)
* [Caffeine](https://github.com/ben-manes/caffeine)

## Compatibility

ScalaCache is available for Scala 2.12.x, 2.13.x, and 3.x.

The JVM must be Java 11 or newer.
