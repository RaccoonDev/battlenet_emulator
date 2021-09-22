package com.devraccoon.starcraft.domain

import io.estatico.newtype.macros.newtype

object maps {

  @newtype case class MapId(value: String)
  @newtype case class MapName(value: String)

  @newtype case class MaxPlayers(value: Int)
  @newtype case class MinPlayers(value: Int)
  @newtype case class MapVersion(value: Int)

  final case class GameMap(id: MapId,
                           name: MapName,
                           maxPlayers: MaxPlayers,
                           minPlayers: MinPlayers)
}
