package scalacache.serialization

trait DummyCodec {

  /**
   * This will never get used, because any cache impl that specify Dummy as their codec target
   * do not need to perform any serialization.
   */
  implicit def anyCodec[A] = new Codec[A, Dummy] {
    override def serialize(value: A): Dummy = ???
    override def deserialize(data: Dummy): A = ???
  }

}
