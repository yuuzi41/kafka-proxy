package org.frostplain.kafkaproxy

import java.util

import org.apache.kafka.common.serialization.Serializer

//todo: implement
class AvroSerializer extends Serializer[Map[String,Any]] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {

  }

  override def serialize(topic: String, data: Map[String, Any]): Array[Byte] = {
    new Array[Byte](0)
  }

  override def close(): Unit = {

  }

}
