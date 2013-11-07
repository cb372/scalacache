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

