package com.devraccoon.starcraft.kafka

import cats.data.NonEmptyVector
import cats.effect.IO
import cats.syntax.foldable._
import com.banno.kafka.{ClientId, GroupId}
import com.devraccoon.starcraft.KafkaOutput
import com.devraccoon.starcraft.KafkaOutput._
import com.devraccoon.starcraft.domain.game.{GameId, GameType, RegionId}
import com.devraccoon.starcraft.domain.maps.MapId
import com.devraccoon.starcraft.domain.player.{Nickname, PlayerId}
import com.devraccoon.starcraft.domain.server.{
  GameStarted,
  PlayerOnline,
  ServerEvent
}
import munit.CatsEffectSuite
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration._

class TestingServerEventKafkaProducer extends CatsEffectSuite {

  override def beforeAll(): Unit =
    KafkaOutput.init.unsafeRunSync()

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
    getProducer(ClientId("server-events-producer"))
      .use(p => recordsToBeWritten.traverse_(p.sendSync))
      .unsafeRunSync()
  }

  test("consumer can read messages") {
    val initialOffsets = Map.empty[TopicPartition, Long]

    getConsumer(ClientId("server-event-consumer"),
                GroupId("unit-test-consumer-group")).use(
      c =>
        c.assign(topicName, initialOffsets) *> c
          .recordStream(1.second)
          .take(2)
          .foreach(r => IO(println(r.value())))
          .compile
          .drain)
  }
}
