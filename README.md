# ScalaCache

[![Join the chat at https://gitter.im/cb372/scalacache](https://badges.gitter.im/cb372/scalacache.svg)](https://gitter.im/cb372/scalacache?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/cb372/scalacache.png?branch=master)](https://travis-ci.org/cb372/scalacache) [![Maven Central](https://img.shields.io/maven-central/v/com.github.cb372/scalacache-core_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Cscalacache)

A facade for the most popular cache implementations, with a simple, idiomatic Scala API.

Use ScalaCache to add caching to any Scala app with the minimum of fuss.

The following cache implementations are supported, and it's easy to plugin your own implementation:
* Google Guava
* Memcached
* Ehcache
* Redis
* [Caffeine](https://github.com/ben-manes/caffeine)
* [cache2k](https://github.com/cache2k/cache2k)
* [OHC](https://github.com/snazy/ohc)

## Documentation

Documentation is available on [the ScalaCache website](https://cb372.github.io/scalacache/).

## Compatibility

ScalaCache is available for Scala 2.11.x, 2.12.x, and 2.13.x.

The JVM must be Java 8 or newer.

## Compiling the documentation

To make a change to the documentation:

1. Make sure that memcached is running on localhost:11211
2. Perform the desired changes
3. Run `sbt doc/makeMicrosite`

