package org.frostplain.kafkaproxy

import java.util.Properties

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

object KafkaController {
  var producer: Option[KafkaProducer[String, Array[Byte]]] = None

  def init(config: Properties): Unit = {
    producer = Some(new KafkaProducer[String, Array[Byte]](config, new StringSerializer, new ByteArraySerializer))
  }
}
