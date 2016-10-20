package scalacache.aerospike

import com.aerospike.client.listener.{DeleteListener, RecordListener, WriteListener}
import com.aerospike.client.{AerospikeException, Key, Record}

import scala.collection.JavaConversions._
import scala.concurrent.Promise

/**
 * Aerospike WriteListener to get write result operation as Future
 * @param p to update with result
 */
class WriteHandler(val p: Promise[Unit]) extends WriteListener {
  override def onFailure(ex: AerospikeException) = p.failure(ex)
  override def onSuccess(key: Key) = p.success(Unit)
}

/**
 * Aerospike ReadListener to get read result as Future
 * @param p to update with value retrieved from cache
 */
class ReadHandler(val p: Promise[Option[Array[Byte]]]) extends RecordListener {
  override def onFailure(ex: AerospikeException) = p.failure(ex)

  override def onSuccess(key: Key, record: Record) =
    p.success(Option(record) map { r => r.bins("cache").asInstanceOf[Array[Byte]] })
}

class DeleteHandler(val p: Promise[Unit]) extends DeleteListener {
  override def onFailure(ex: AerospikeException): Unit = p.failure(ex)
  override def onSuccess(key: Key, existed: Boolean) = p.success(Unit)
}