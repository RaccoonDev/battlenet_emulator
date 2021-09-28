package com.devraccoon.starcraft

import cats.effect.{IO, Resource}
import com.banno.kafka.{BootstrapServers, GroupId, SchemaRegistryUrl}
import com.banno.kafka.admin.AdminApi
import com.banno.kafka.consumer.ConsumerApi
import com.banno.kafka.producer.ProducerApi
import com.banno.kafka.schemaregistry.SchemaRegistryApi
import com.devraccoon.starcraft.domain.server.ServerEvent
import com.sksamuel.avro4s.RecordFormat
import org.apache.kafka.clients.admin.NewTopic
import com.devraccoon.starcraft.utils.avro._

object KafkaOutput {

  case class EventId(id: String)

  implicit val eventIdRecordFormat: RecordFormat[EventId] =
    RecordFormat[EventId]
  implicit val serverEventRecordFormat: RecordFormat[ServerEvent] =
    RecordFormat[ServerEvent]

  val topic = new NewTopic("battlenet.server.events.v1", 1, 1.toShort)
  val kafkaBootstrapServer = "localhost:9092"

  val topicName: String = topic.name()
  val schemaRegistryUri = "http://localhost:8081"

  val init: IO[Unit] =
    for {
      _ <- AdminApi
        .createTopicsIdempotent[IO](kafkaBootstrapServer, topic :: Nil)
      _ <- SchemaRegistryApi
        .register[IO, EventId, ServerEvent](
          schemaRegistryUri,
          topicName
        )
    } yield ()

  /**
    * Get kafka producer resource
    * @param clientId tuple of config key and config value. It is best defined using helper object ClientId("client-id-value-here")
    * @return resource with access to a kafka producer
    */
  def getProducer(clientId: (String, String))
    : Resource[IO, ProducerApi[IO, EventId, ServerEvent]] =
    ProducerApi.Avro.Generic
      .resource[IO](
        BootstrapServers(kafkaBootstrapServer),
        SchemaRegistryUrl(schemaRegistryUri),
        clientId
      )
      .map(_.toAvro4s[EventId, ServerEvent])

  /**
    * Get kafka consumer resource
    * @param clientId tuple of config key and config value. It is best defined using helper object ClientId("client-id-value-here")
    * @param groupId consumer group id
    * @return resource with access to a kafka consumer
    */
  def getConsumer(
      clientId: (String, String),
      groupId: GroupId): Resource[IO, ConsumerApi[IO, EventId, ServerEvent]] =
    ConsumerApi.Avro4s.resource[IO, EventId, ServerEvent](
      BootstrapServers(kafkaBootstrapServer),
      SchemaRegistryUrl(schemaRegistryUri),
      clientId,
      groupId
    )

}
