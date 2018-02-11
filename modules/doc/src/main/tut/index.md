---
layout: home
title: "ScalaCache"
technologies:
 - first: ["Scala", "As the name implies ScalaCache is written in Scala."]
---

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
