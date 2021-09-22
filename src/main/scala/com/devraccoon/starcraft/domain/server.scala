package com.devraccoon.starcraft.domain

import cats.data.NonEmptySeq
import com.devraccoon.starcraft.domain.game.{Game, GameId, GameType, RegionId}
import com.devraccoon.starcraft.domain.maps.MapId
import com.devraccoon.starcraft.domain.player.{
  Nickname,
  PlayerId,
  PlayerRegistered
}
import com.devraccoon.starcraft.utils._

import scala.annotation.tailrec

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
                   lookingForGame: Set[GameSearch],
                   runningGames: Set[Game]) {
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
      bringPlayersOnline(currentTime, newOnlinePlayerIds)
    }

    def bringPlayersOnline(currentTime: java.time.Instant,
                           newOnlinePlayerIds: Set[PlayerId]): State = {
      copy(registeredPlayers = registeredPlayers ++ withLastActivity(
             newOnlinePlayerIds,
             currentTime),
           onlinePlayerIds = onlinePlayerIds ++ newOnlinePlayerIds)
    }

    def startSomePlayersLookingForGame(
        currentTime: java.time.Instant): State = {
      val subsetOfOnlinePlayers: Set[PlayerId] = onlinePlayerIds
        .diff(lookingForGame.map(_.id))
        .toList
        .takeRandomPercentOfElements(10)
        .toSet

      startLookingForGame(currentTime, subsetOfOnlinePlayers)
    }

    def startLookingForGame(currentTime: java.time.Instant,
                            onlinePlayers: Set[PlayerId],
                            gameType: GameType = GameType.random): State = {
      copy(
        registeredPlayers = registeredPlayers ++ withLastActivity(onlinePlayers,
                                                                  currentTime),
        lookingForGame = lookingForGame ++ onlinePlayers.map(
          GameSearch(_, gameType))
      )
    }

    def startMatchedGames(currentTime: java.time.Instant): State = {
      lookingForGame.groupBy(_.gameType).foldLeft(this) {
        case (st, (gameType, gameSearches)) =>
          gameType match {
            case GameType.OneVsOne =>
              startOneVsOne(currentTime, st, gameSearches.toList)
            case GameType.TwoVsTwo     => ???
            case GameType.ThreeVsThree => ???
            case GameType.FourVsFour   => ???
            case GameType.FreeForAll   => ???
          }
      }
    }

    @tailrec
    private def startOneVsOne(currentTime: java.time.Instant,
                              state: State,
                              gameSearch: List[GameSearch]): State = {
      gameSearch match {
        case playerOne :: playerTwo :: others =>
          startOneVsOne(
            currentTime,
            state.copy(
              runningGames = state.runningGames + Game(
                currentTime,
                NonEmptySeq.of(playerOne.id, playerTwo.id),
                MapId.newRandom,
                GameId.newRandom,
                RegionId.newRandom,
                GameType.OneVsOne
              )
            ),
            others
          )
        case _ => state
      }
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
    def empty: State = State(Map.empty, Set.empty, Set.empty, Set.empty)
  }
}
