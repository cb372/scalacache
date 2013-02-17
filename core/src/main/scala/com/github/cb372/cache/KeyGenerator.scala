package com.github.cb372.cache

trait KeyGenerator {

  def toCacheKey(className: String, method: String, paramss: Seq[Seq[Any]]): String
  
}

object KeyGenerator {

  implicit val defaultGenerator: KeyGenerator = new KeyGenerator {
    val shortSeparator = "_"
    val longSeparator = "__"
    def toCacheKey(className: String, method: String, paramss: Seq[Seq[Any]]): String = 
      className + longSeparator + method + longSeparator + paramss.map(_.mkString(shortSeparator)).mkString(longSeparator)
  }

}

