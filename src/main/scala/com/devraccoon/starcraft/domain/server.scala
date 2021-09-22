package com.devraccoon.starcraft.domain

import cats.data.NonEmptyVector
import com.devraccoon.starcraft.domain.game.{Game, GameId, GameType, RegionId}
import com.devraccoon.starcraft.domain.maps.{GameMap, MapId}
import com.devraccoon.starcraft.domain.player.{
  Nickname,
  PlayerId,
  RegisterPlayer
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

  sealed trait ServerEvent {
    def eventTime: java.time.Instant
  }

  final case class GameStarted(
      eventTime: java.time.Instant,
      gameId: GameId,
      playerIds: NonEmptyVector[PlayerId],
      mapId: MapId,
      regionId: RegionId,
      gameType: GameType
  ) extends ServerEvent

  final case class GameFinished(
      eventTime: java.time.Instant,
      gameId: GameId
  ) extends ServerEvent

  final case class PlayerRegistered(
      eventTime: java.time.Instant,
      playerId: PlayerId,
      nickname: Nickname
  ) extends ServerEvent

  final case class PlayerOnline(
      eventTime: java.time.Instant,
      playerId: PlayerId,
      nickname: Nickname
  ) extends ServerEvent

  final case class PlayerIsLookingForAGame(
      eventTime: java.time.Instant,
      playerId: PlayerId,
      gameType: GameType
  ) extends ServerEvent

  final case class PlayerOffline(
      eventTime: java.time.Instant,
      playerId: PlayerId,
      nickname: Nickname
  ) extends ServerEvent

  case class State(registeredPlayers: Map[PlayerId, PlayerInfo],
                   onlinePlayerIds: Set[PlayerId],
                   lookingForGame: Set[GameSearch],
                   runningGames: Set[Game],
                   availableMaps: Vector[GameMap],
                   notDispatchedGameEvents: List[ServerEvent]) {

    import State._

    def registerGameMaps(gameMaps: Vector[GameMap]): State =
      copy(availableMaps = gameMaps)

    def addRegisteredPlayer(event: RegisterPlayer): State =
      copy(
        registeredPlayers = registeredPlayers + (event.id -> PlayerInfo(
          event.id,
          event.nickname,
          event.registrationTime,
          event.registrationTime)),
        notDispatchedGameEvents = PlayerRegistered(
          event.registrationTime,
          event.id,
          event.nickname
        ) +: notDispatchedGameEvents
      )

    def bringSomePlayersOnline(currentTime: java.time.Instant): State = {
      val offlinePlayerIds =
        registeredPlayers.keySet.diff(onlinePlayerIds).toList
      val newOnlinePlayerIds =
        offlinePlayerIds.takeRandomPercentOfElements(20).toSet
      bringPlayersOnline(currentTime, newOnlinePlayerIds)
    }

    def bringPlayersOnline(currentTime: java.time.Instant,
                           newOnlinePlayerIds: Set[PlayerId]): State = {
      copy(
        registeredPlayers = registeredPlayers ++ withLastActivity(
          registeredPlayers,
          newOnlinePlayerIds,
          currentTime),
        onlinePlayerIds = onlinePlayerIds ++ newOnlinePlayerIds,
        notDispatchedGameEvents = notDispatchedGameEvents ++ newOnlinePlayerIds
          .map(p => PlayerOnline(currentTime, p, registeredPlayers(p).nickname))
      )
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
                            playersWhoStartedLookingForAGame: Set[PlayerId],
                            gameType: GameType = GameType.random): State = {
      copy(
        registeredPlayers = registeredPlayers ++ withLastActivity(
          registeredPlayers,
          playersWhoStartedLookingForAGame,
          currentTime),
        lookingForGame = lookingForGame ++ playersWhoStartedLookingForAGame.map(
          GameSearch(_, gameType)),
        notDispatchedGameEvents = notDispatchedGameEvents ++ playersWhoStartedLookingForAGame
          .map(
            p =>
              PlayerIsLookingForAGame(
                currentTime,
                p,
                gameType
            ))
      )
    }

    def startMatchedGames(currentTime: java.time.Instant): State = {
      lookingForGame.groupBy(_.gameType).foldLeft(this) {
        case (st, (gameType, gameSearches)) =>
          gameType match {
            case GameType.OneVsOne =>
              startGame(currentTime, gameType, st, gameSearches.toVector)
            case GameType.TwoVsTwo =>
              startGame(currentTime, gameType, st, gameSearches.toVector)
            case GameType.ThreeVsThree =>
              startGame(currentTime, gameType, st, gameSearches.toVector)
            case GameType.FourVsFour =>
              startGame(currentTime, gameType, st, gameSearches.toVector)
          }
      }
    }

    def dispatchAllEvents: (State, List[ServerEvent]) =
      (copy(notDispatchedGameEvents = List.empty), notDispatchedGameEvents)
  }

  object State {
    def empty: State =
      State(Map.empty,
            Set.empty,
            Set.empty,
            Set.empty,
            Vector.empty,
            List.empty)

    private def withLastActivity(
        registeredPlayers: Map[PlayerId, PlayerInfo],
        affectedIds: Set[PlayerId],
        currentTime: java.time.Instant): Map[PlayerId, PlayerInfo] =
      if (affectedIds.isEmpty)
        Map.empty
      else
        registeredPlayers.view
          .filterKeys(affectedIds.contains)
          .mapValues(_.withLastActivity(currentTime))
          .toMap

    @tailrec
    private def startGame(currentTime: java.time.Instant,
                          gameType: GameType,
                          state: State,
                          gameSearch: Vector[GameSearch]): State = {
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
            val newGame = Game(
              currentTime,
              NonEmptyVector.fromVectorUnsafe(players.map(_.id)),
              m.id,
              GameId.newRandom,
              RegionId.newRandom,
              gameType
            )
            startGame(
              currentTime,
              gameType,
              state.copy(
                runningGames = state.runningGames + newGame,
                lookingForGame = state.lookingForGame -- players,
                notDispatchedGameEvents = GameStarted(
                  newGame.startTime,
                  newGame.gameId,
                  newGame.playerIds,
                  newGame.mapId,
                  newGame.regionId,
                  newGame.gameType
                ) +: state.notDispatchedGameEvents
              ),
              others
            )
          case None =>
            // here should be an event about missing maps for this type of game
            state
        }
      }
    }
  }
}
