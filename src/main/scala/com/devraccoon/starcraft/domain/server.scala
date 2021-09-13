package com.devraccoon.starcraft.domain

import com.devraccoon.starcraft.domain.game.GameType
import com.devraccoon.starcraft.domain.player.{
  Nickname,
  PlayerId,
  PlayerRegistered
}
import com.devraccoon.starcraft.utils._

object server {
  case class PlayerInfo(id: PlayerId,
                        nickname: Nickname,
                        registrationTime: java.time.Instant,
                        lastActivity: java.time.Instant) {
    def withLastActivity(activityTime: java.time.Instant): PlayerInfo = {
      copy(lastActivity = activityTime)
    }
  }

  case class GameSearch(id: PlayerId, gameType: GameType)

  case class State(registeredPlayers: Map[PlayerId, PlayerInfo],
                   onlinePlayerIds: Set[PlayerId],
                   lookingForGame: Set[GameSearch]) {
    def addRegisteredPlayer(event: PlayerRegistered): State =
      copy(registeredPlayers + (event.id -> PlayerInfo(event.id,
                                                       event.nickname,
                                                       event.registrationTime,
                                                       event.registrationTime)),
           onlinePlayerIds)

    def bringSomePlayersOnline(currentTime: java.time.Instant): State = {
      val offlinePlayerIds =
        registeredPlayers.keySet.diff(onlinePlayerIds).toList
      val newOnlinePlayerIds =
        offlinePlayerIds.takeRandomPercentOfElements(20).toSet
      copy(registeredPlayers = registeredPlayers ++ withLastActivity(
             newOnlinePlayerIds,
             currentTime),
           onlinePlayerIds = newOnlinePlayerIds)
    }

    def startLookingForGame(currentTime: java.time.Instant): State = {
      val subsetOfOnlinePlayers: Set[PlayerId] = onlinePlayerIds
        .diff(lookingForGame.map(_.id))
        .toList
        .takeRandomPercentOfElements(10)
        .toSet

      copy(
        registeredPlayers = registeredPlayers ++ withLastActivity(
          subsetOfOnlinePlayers,
          currentTime),
        lookingForGame = lookingForGame ++ subsetOfOnlinePlayers.map(
          GameSearch(_, GameType.random))
      )
    }

    private def withLastActivity(
        affectedIds: Set[PlayerId],
        currentTime: java.time.Instant): Map[PlayerId, PlayerInfo] =
      if (affectedIds.isEmpty)
        Map.empty
      else
        registeredPlayers.view
          .filterKeys(affectedIds.contains)
          .mapValues(_.withLastActivity(currentTime))
          .toMap
  }

  object State {
    def empty: State = State(Map.empty, Set.empty, Set.empty)
  }
}
