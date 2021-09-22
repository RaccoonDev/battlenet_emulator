package com.devraccoon.starcraft.domain

import io.estatico.newtype.macros.newtype

import java.util.UUID

object maps {
  @newtype case class MapId(value: UUID)
  object MapId {
    def newRandom: MapId = MapId(UUID.randomUUID())
  }

  @newtype case class MapName(value: String)

  @newtype case class MaxPlayers(value: Int)
  @newtype case class MinPlayers(value: Int)
  @newtype case class MapVersion(value: Int)

  final case class Map(id: MapId,
                       creationTime: java.time.Instant,
                       version: MapVersion,
                       name: MapName,
                       maxPlayers: MaxPlayers,
                       minPlayers: MinPlayers)
}
