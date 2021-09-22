package com.devraccoon.starcraft.domain

import cats.data.NonEmptyList
import com.devraccoon.starcraft.domain.game.{Game, GameId, GameType, RegionId}
import com.devraccoon.starcraft.domain.maps.GameMap
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
                   runningGames: Set[Game],
                   availableMaps: Vector[GameMap]) {
    def registerGameMaps(gameMaps: Vector[GameMap]): State =
      copy(availableMaps = gameMaps)

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
              startGame(currentTime, gameType, st, gameSearches.toList)
            case GameType.TwoVsTwo =>
              startGame(currentTime, gameType, st, gameSearches.toList)
            case GameType.ThreeVsThree =>
              startGame(currentTime, gameType, st, gameSearches.toList)
            case GameType.FourVsFour =>
              startGame(currentTime, gameType, st, gameSearches.toList)
          }
      }
    }

    @tailrec
    private def startGame(currentTime: java.time.Instant,
                          gameType: GameType,
                          state: State,
                          gameSearch: List[GameSearch]): State = {
      val numberOfPlayers = gameType.numberOfPlayers

      if (gameSearch.length < numberOfPlayers) state
      else {
        val (players, others) =
          gameSearch.splitAt(numberOfPlayers)

        state.availableMaps
          .filter(m =>
            numberOfPlayers >= m.minPlayers.value && numberOfPlayers <= m.maxPlayers.value)
          .takeOneRandomElement() match {
          case Some(m) =>
            startGame(
              currentTime,
              gameType,
              state.copy(
                runningGames = state.runningGames + Game(
                  currentTime,
                  NonEmptyList.fromListUnsafe(players.map(_.id)),
                  m.id,
                  GameId.newRandom,
                  RegionId.newRandom,
                  gameType
                ),
                lookingForGame = state.lookingForGame -- players
              ),
              others
            )
          case None =>
            // here should be an event about missing maps for this type of game
            state
        }

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
    def empty: State =
      State(Map.empty, Set.empty, Set.empty, Set.empty, Vector.empty)
  }
}
