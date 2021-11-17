---
layout: docs
title: Serialization
---

## Serialization

If you are using a cache implementation that does not store its data locally (like [Memcached](cache-implementations.html#memcached) and [Redis](cache-implementations.html#redis)), you will need to choose a codec in order to serialize your data to bytes.

### Binary codec

ScalaCache provides efficient `Codec` instances for all primitive types, and also an implementation for objects based on Java serialization.

To use this codec, you need one import:

```scala mdoc:fail:silent
import scalacache.serialization.binary._
```

### JSON codec

If you want to serialize your values as JSON, you can use ScalaCache's [circe](https://circe.github.io/circe/) integration.

You will need to add a dependency on the scalacache-circe module:

```
libraryDependencies += "com.github.cb372" %% "scalacache-circe" % "0.28.0"
```

Then import the codec:

```scala mdoc:fail:silent
import scalacache.serialization.circe._
```

If your cache holds values of type `Cat`, you will also need a Circe `Encoder[Cat]` and `Decoder[Cat]` in implicit scope. The easiest way to do this is to ask circe to automatically derive them:

```scala
import io.circe.generic.auto._
```

but if you are worried about performance, it's better to derive them semi-automatically:

```scala
import io.circe._
import io.circe.generic.semiauto._
implicit val catEncoder: Encoder[Cat] = deriveEncoder[Cat]
implicit val catDecoder: Decoder[Cat] = deriveDecoder[Cat]
```

For more information, please consult the [circe docs](https://circe.github.io/circe/).

### Custom Codec

If you want to use a custom `Codec` for your object of type `A`, simply implement an instance of `Codec[A]` and make sure it
is in scope at your `get`/`put` call site.

### Compression of `Codec[A]`

If you want to compress your serialized data before sending it to your cache, ScalaCache has a built-in `GZippingBinaryCodec[A]` mix-in
trait that can be used to decorate another codec. It will automatically apply GZip compression to the encoded value if the `Array[Byte]` representation is above a `sizeThreshold`. It also takes care of properly decompressing data upon retrieval.

To use it, simply extend your `Codec[A]` with `GZippingBinaryCodec[A]` **last** (it should be the right-most extended trait).

If you want to use GZip compression with the standard ScalaCache binary codec can either `import scalacache.serialization.gzip.GZippingJavaSerializationCodec._` or provide an implicit `GZippingJavaAnyBinaryCodec` at the cache call site.
