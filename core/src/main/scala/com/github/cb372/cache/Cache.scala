package com.github.cb372.cache

trait Cache {
  def get[V](key: String): Option[V]
  def put[V](key: String, value: V): Unit
}

