package com.devraccoon.starcraft.domain

import com.devraccoon.starcraft.domain.player.{
  Nickname,
  PlayerId,
  PlayerRegistered
}

object server {
  case class State(registeredPlayers: Map[PlayerId, Nickname],
                   onlinePlayers: Set[PlayerId]) {
    def addRegisteredPlayer(event: PlayerRegistered): State =
      copy(registeredPlayers + (event.id -> event.nickname), onlinePlayers)
  }

  object State {
    def empty: State = State(Map.empty, Set.empty)
  }
}
