package com.github.cb372.cache

import Cacheable._

case class User(id: Int, name: String)

object Sample extends App {

  class UserRepository {
    implicit val cacheConfig = CacheConfig(new SimpleCache(), KeyGenerator.defaultGenerator)
  
    def getUser(id: Int): User = {
      cacheable { 
        // Do DB lookup here...
        User(id, s"user${id}")
      }
    }
  }

  override def main(args: Array[String]) {
    val userRepo = new UserRepository()
    println(userRepo.getUser(3))
    println(userRepo.getUser(3))
  }
}

class SimpleCache extends Cache {

  private val mmap = collection.mutable.Map[String, Any]()

  def get[V](key: String): Option[V] = {
    val value = mmap.get(key)
    println(s"get(key = ${key}, return: ${value})")
    value.asInstanceOf[Option[V]]
  }

  def put[V](key: String, value: V): Unit = {
    println(s"put(${key} -> ${value}")
    mmap.put(key, value)
  }

}
