package com.devraccoon.starcraft.domain

import cats.data.NonEmptySeq
import enumeratum._
import io.estatico.newtype.macros.newtype

import java.util.UUID

object game {
  import player._
  import maps._
  import resources._

  sealed trait GameType extends EnumEntry

  object GameType extends Enum[GameType] {
    val values: IndexedSeq[GameType] = findValues

    case object OneVsOne extends GameType
    case object TwoVsTwo extends GameType
    case object ThreeVsThree extends GameType
    case object FourVsFour extends GameType
    case object FreeForAll extends GameType
  }

  @newtype case class GameId(value: UUID)
  @newtype case class RegionId(value: UUID)
  final case class Game(startTime: java.time.Instant,
                        playerId: NonEmptySeq[PlayerId],
                        mapId: MapId,
                        gameId: GameId,
                        regionId: RegionId,
                        gameType: GameType)

  sealed trait GameEvent {
    def eventTime: java.time.Instant
    def playerId: PlayerId
  }

  sealed trait EntityId
  @newtype case class UnitIdValue(value: UUID)
  final case class UnitId(value: UnitIdValue) extends EntityId
  @newtype case class BuildingIdValue(value: UUID)
  final case class BuildingId(value: BuildingIdValue) extends EntityId

  @newtype case class Health(value: Long)
  @newtype case class Attack(value: Long)

  sealed trait UnitType
  case class CSV(health: Health, attack: Attack) extends UnitType
  case class Marine(health: Health, attack: Attack) extends UnitType
  case class Probe(health: Health, attack: Attack) extends UnitType
  case class Zealot(health: Health, attack: Attack) extends UnitType
  case class Zerling(health: Health, attack: Attack) extends UnitType

  sealed trait ConstructionState extends EnumEntry
  object ConstructionState extends Enum[ConstructionState] {
    val values: IndexedSeq[ConstructionState] = findValues

    case object ConstructionRequested extends ConstructionState
    case object ConstructionDone extends ConstructionState
  }

  final case class UnitConstructionEvent(
      eventTime: java.time.Instant,
      playerId: PlayerId,
      unitType: UnitType,
      unitId: UnitId,
      unitConstructionEventType: ConstructionState,
      cost: Money)
      extends GameEvent

  sealed trait BuildingType
  case class CommandCenter(health: Health, attack: Attack) extends UnitType
  case class Nexus(health: Health, attack: Attack) extends UnitType
  case class Hatchery(health: Health, attack: Attack) extends UnitType

  final case class BuildingConstructionEvent(
      eventTime: java.time.Instant,
      playerId: PlayerId,
      buildingType: BuildingType,
      buildingId: BuildingId,
      buildingConstructionEventType: ConstructionState,
      cost: Money)
      extends GameEvent

  final case class FightEvent(eventTime: java.time.Instant,
                              playerId: PlayerId,
                              attackingEntityId: EntityId,
                              defendingEntityId: EntityId,
                              damageDone: Long,
                              isDestroyed: Boolean)
      extends GameEvent
}
