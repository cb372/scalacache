# Cacheable

An experimental attempt to recreate the behaviour of Spring's `@Cacheable` annotation, using Scala macros instead of AOP.

Example usage:

```scala 
implicit val cacheConfig = CacheConfig(new MyCache(), KeyGenerator.defaultGenerator)

def getUser(id: Int): User = {  
  cacheable { 
    // Do DB lookup here...
    User(id, s"user${id}")
  }                    
}
```

This will memoize the result of the `cacheable` block in a cache of your choice.

The cache key is built automatically from the class name, the name of the enclosing method (`getUser`), and the values of all of the method's parameters.

## TODO

* Stabilise the API
* Write some tests
* Provide a few useful cache implementations: Guava, Redis, Memached, ...
