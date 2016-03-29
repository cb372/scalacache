package scalacache.serialization

/**
 * Provides plain Java serialization in addition to simple primitive codecs.
 */
object JavaSerializationCodecs extends JavaSerializationCodecs

// We put Java serialisation last to prioritise the implicits
trait JavaSerializationCodecs extends BaseCodecs with JavaSerializationCodec