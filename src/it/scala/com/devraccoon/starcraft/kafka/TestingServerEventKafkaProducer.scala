package com.devraccoon.starcraft.kafka

import cats.data.NonEmptyVector
import cats.effect.{IO, Resource}
import cats.syntax.foldable._
import com.banno.kafka.{BootstrapServers, ClientId, GroupId, SchemaRegistryUrl}
import com.banno.kafka.admin.AdminApi
import com.banno.kafka.consumer.ConsumerApi
import com.banno.kafka.producer.ProducerApi
import com.banno.kafka.schemaregistry.SchemaRegistryApi
import com.devraccoon.starcraft.domain.game.{GameId, GameType, RegionId}
import com.devraccoon.starcraft.domain.maps.MapId
import com.devraccoon.starcraft.domain.player.{Nickname, PlayerId}
import com.devraccoon.starcraft.domain.server.{
  GameStarted,
  PlayerOnline,
  ServerEvent
}
import com.sksamuel.avro4s.RecordFormat
import munit.CatsEffectSuite
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerRecord

import java.time.Instant
import java.util.UUID
import com.devraccoon.starcraft.utils.avro._
import org.apache.kafka.common.TopicPartition

import scala.concurrent.duration._

class TestingServerEventKafkaProducer extends CatsEffectSuite {

  case class EventId(id: String)

  implicit val eventIdRecordFormat: RecordFormat[EventId] =
    RecordFormat[EventId]
  implicit val serverEventRecordFormat: RecordFormat[ServerEvent] =
    RecordFormat[ServerEvent]

  val topic = new NewTopic("battlenet.server.events.v1", 1, 1.toShort)
  val kafkaBootstrapServer = "localhost:9092"

  AdminApi
    .createTopicsIdempotent[IO](kafkaBootstrapServer, topic :: Nil)
    .unsafeRunSync()

  val topicName: String = topic.name()
  val schemaRegistryUri = "http://localhost:8081"

  SchemaRegistryApi
    .register[IO, EventId, ServerEvent](
      schemaRegistryUri,
      topicName
    )
    .unsafeRunSync()

  val producer: Resource[IO, ProducerApi[IO, GenericRecord, GenericRecord]] =
    ProducerApi.Avro.Generic.resource[IO](
      BootstrapServers(kafkaBootstrapServer),
      SchemaRegistryUrl(schemaRegistryUri),
      ClientId("server-events-producer")
    )

  val consumer: Resource[IO, ConsumerApi[IO, EventId, ServerEvent]] =
    ConsumerApi.Avro4s.resource[IO, EventId, ServerEvent](
      BootstrapServers(kafkaBootstrapServer),
      SchemaRegistryUrl(schemaRegistryUri),
      ClientId("server-events-consumer"),
      GroupId("unit-test-consumer-group")
    )

  val recordsToBeWritten: Seq[ProducerRecord[EventId, ServerEvent]] = Seq(
    {
      val playerId = UUID.randomUUID()
      new ProducerRecord(
        topicName,
        EventId(s"player|$playerId"),
        PlayerOnline(Instant.now(), PlayerId(playerId), Nickname("bob")))
    }, {
      val gameId = UUID.randomUUID()
      new ProducerRecord(
        topicName,
        EventId(s"game|$gameId"),
        GameStarted(
          Instant.now(),
          GameId(gameId),
          NonEmptyVector.one(PlayerId(UUID.randomUUID())),
          MapId("lost_temple"),
          RegionId(UUID.randomUUID()),
          GameType.OneVsOne
        )
      )
    }
  )

  test("producer can write messages") {
    producer
      .map(_.toAvro4s[EventId, ServerEvent])
      .use(p => recordsToBeWritten.traverse_(p.sendSync))
      .unsafeRunSync()
  }

  test("consumer can read messages") {
    val initialOffsets = Map.empty[TopicPartition, Long]

    consumer.use(
      c =>
        c.assign(topicName, initialOffsets) *> c
          .recordStream(1.second)
          .take(15)
          .foreach(r => IO(println(r.value())))
          .compile
          .drain)
  }
}
