package com.devraccoon.starcraft.kafka

import cats.effect.{IO, Resource}
import com.banno.kafka.{BootstrapServers, ClientId, GroupId, SchemaRegistryUrl}
import munit.CatsEffectSuite
import com.banno.kafka.producer._
import com.banno.kafka.admin._
import com.banno.kafka.consumer.ConsumerApi
import com.banno.kafka.schemaregistry.SchemaRegistryApi
import com.sksamuel.avro4s.RecordFormat
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import scala.concurrent.duration._

class KafkaTestingExampleSuite extends CatsEffectSuite {

  case class Customer(name: String, address: String)
  case class CustomerId(id: String)

  implicit val CustomerRecordFormat: RecordFormat[Customer] =
    com.sksamuel.avro4s.RecordFormat[Customer]
  implicit val CustomerIdRecordFormat: RecordFormat[CustomerId] =
    com.sksamuel.avro4s.RecordFormat[CustomerId]

  val topic = new NewTopic("customers.v1", 1, 1.toShort)
  val kafkaBootstrapServers = "localhost:9092"

  AdminApi
    .createTopicsIdempotent[IO](kafkaBootstrapServers, topic :: Nil)
    .unsafeRunSync()

  val topicName: String = topic.name()
  val schemaRegistryUri = "http://localhost:8081"

  SchemaRegistryApi
    .register[IO, CustomerId, Customer](
      schemaRegistryUri,
      topicName
    )
    .unsafeRunSync()

  val producer: Resource[IO, ProducerApi[IO, GenericRecord, GenericRecord]] =
    ProducerApi.Avro.Generic.resource[IO](
      BootstrapServers(kafkaBootstrapServers),
      SchemaRegistryUrl(schemaRegistryUri),
      ClientId("producer-example")
    )

  val consumer: Resource[IO, ConsumerApi[IO, CustomerId, Customer]] =
    ConsumerApi.Avro4s.resource[IO, CustomerId, Customer](
      BootstrapServers(kafkaBootstrapServers),
      SchemaRegistryUrl(schemaRegistryUri),
      ClientId("consumer-example"),
      GroupId("consumer-example-group")
    )

  val recordsToBeWritten: Seq[ProducerRecord[CustomerId, Customer]] =
    (1 to 10)
      .map(
        a =>
          new ProducerRecord(topicName,
                             CustomerId(a.toString),
                             Customer(s"name-$a", s"address-$a")))
      .toVector

  import cats.syntax.foldable._

  test("producer can write messages") {
    producer
      .map(_.toAvro4s[CustomerId, Customer])
      .use(p => recordsToBeWritten.traverse_(p.sendSync))
      .unsafeRunSync()
  }

  test("consumer can read from beginning") {
    val initialOffsets = Map.empty[TopicPartition, Long]

    val messages = consumer
      .use(
        c =>
          c.assign(topicName, initialOffsets) *> c
            .recordStream(1.second)
            .take(5)
            .compile
            .toVector)
      .unsafeRunSync()

    assertEquals(messages.length, 5)
  }

}
