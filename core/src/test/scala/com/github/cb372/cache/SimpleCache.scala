package com.github.cb372.cache

/**
 *
 * Author: c-birchall
 * Date:   13/11/07
 */
class SimpleCache extends Cache {

  val mmap = collection.mutable.Map[String, Any]()

  def get[V](key: String): Option[V] = {
    val value = mmap.get(key)
    value.asInstanceOf[Option[V]]
  }

  def put[V](key: String, value: V): Unit = {
    mmap.put(key, value)
  }

}
