package com.devraccoon.starcraft.kafka

import cats.data.NonEmptyVector
import com.devraccoon.starcraft.domain.game.{GameId, GameType, RegionId}
import com.devraccoon.starcraft.domain.maps.MapId
import com.devraccoon.starcraft.domain.player.{Nickname, PlayerId}
import com.devraccoon.starcraft.domain.server.{
  GameStarted,
  PlayerOnline,
  PlayerRegistered,
  ServerEvent
}
import com.sksamuel.avro4s.{Record, RecordFormat}
import munit.CatsEffectSuite

import java.time.Instant
import java.util.UUID

// If you remove the line below because idea said that this
// import is not used, the code won't compile.
import com.devraccoon.starcraft.utils.avro._
import com.devraccoon.starcraft.utils._

class ServerEventRecordsSuite extends CatsEffectSuite {

  test("PlayerOnline events can be written to kafka record") {

    val PlayerOnlineRecordFormat: RecordFormat[PlayerOnline] =
      RecordFormat[PlayerOnline]

    val record: Record = PlayerOnlineRecordFormat.to(
      PlayerOnline(Instant.now(),
                   PlayerId(UUID.randomUUID()),
                   Nickname("Unit Test Rocks")))

    assert(record != null)

    println(record.toString)
  }

  test("ServerEvents can be written to kafka record") {

    val serverEventRf = RecordFormat[ServerEvent]

    val started = GameStarted(
      Instant.now().millisPrecision,
      GameId(UUID.randomUUID()),
      NonEmptyVector.fromVectorUnsafe(Vector(PlayerId(UUID.randomUUID()))),
      MapId("lost_temple"),
      RegionId(UUID.randomUUID()),
      GameType.OneVsOne
    )

    val gameStartedRecord = serverEventRf.to(started)
    assert(gameStartedRecord != null)
    println(gameStartedRecord)
    assertEquals(serverEventRf.from(gameStartedRecord), started)

    val registered =
      PlayerRegistered(Instant.now().millisPrecision,
                       PlayerId(UUID.randomUUID()),
                       Nickname("bb"), None)
    val playerRegisteredRecord = serverEventRf.to(registered)

    assert(playerRegisteredRecord != null)
    println(playerRegisteredRecord)
    assertEquals(serverEventRf.from(playerRegisteredRecord), registered)
  }

}
