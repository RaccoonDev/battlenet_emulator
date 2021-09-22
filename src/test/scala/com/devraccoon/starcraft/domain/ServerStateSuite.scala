package com.devraccoon.starcraft.domain

import com.devraccoon.starcraft.domain.game.GameType
import com.devraccoon.starcraft.domain.maps.{
  GameMap,
  MapId,
  MapName,
  MaxPlayers,
  MinPlayers
}
import com.devraccoon.starcraft.domain.player.{
  Nickname,
  PlayerId,
  PlayerRegistered
}
import com.devraccoon.starcraft.domain.server.State
import munit.FunSuite

class ServerStateSuite extends FunSuite {

  private def newPlayer(nickname: String): PlayerRegistered =
    PlayerRegistered(PlayerId.newRandom,
                     java.time.Instant.now(),
                     Nickname(nickname))

  private val playerA = newPlayer("player_a")
  private val playerB = newPlayer("player_b")
  private val playerC = newPlayer("player_c")
  private val playerD = newPlayer("player_d")
  private val playerE = newPlayer("player_e")
  private val playerF = newPlayer("player_f")
  private val playerG = newPlayer("player_g")
  private val playerH = newPlayer("player_h")

  private val state =
    State.empty
      .registerGameMaps(
        Vector(
          GameMap(MapId("lost_temple"),
                  MapName("Lost Temple"),
                  MaxPlayers(4),
                  MinPlayers(2))))
      .addRegisteredPlayer(playerA)
      .addRegisteredPlayer(playerB)
      .addRegisteredPlayer(playerC)
      .addRegisteredPlayer(playerD)
      .bringPlayersOnline(java.time.Instant.now(),
                          Set(playerA.id, playerB.id, playerC.id, playerD.id))

  private val onePlayerOnline = state
    .startLookingForGame(java.time.Instant.now(),
                         Set(playerA.id),
                         GameType.OneVsOne)
  private val twoPlayersOnline = onePlayerOnline
    .startLookingForGame(java.time.Instant.now(),
                         Set(playerB.id),
                         GameType.OneVsOne)

  private val fourPlayersLookingForTwoGames = twoPlayersOnline
    .startLookingForGame(java.time.Instant.now(),
                         Set(playerC.id, playerD.id),
                         GameType.OneVsOne)

  private val sixPlayersLookingForAGame = fourPlayersLookingForTwoGames
    .startLookingForGame(java.time.Instant.now(),
                         Set(playerE.id, playerF.id),
                         GameType.TwoVsTwo)

  private val eightPlayersWithOneTwoVsTwoGameSearch = sixPlayersLookingForAGame
    .startLookingForGame(java.time.Instant.now(),
                         Set(playerG.id, playerH.id),
                         GameType.TwoVsTwo)

  test("No matches when nobody online") {
    val matchedGames =
      state.startMatchedGames(java.time.Instant.now()).runningGames
    assertEquals(matchedGames.size, 0)
  }

  test("No matches when only one player online") {
    val matchedGames =
      onePlayerOnline.startMatchedGames(java.time.Instant.now()).runningGames
    assertEquals(matchedGames.size, 0)
  }

  test("Game is started when a both players are online") {
    val matchedGames =
      twoPlayersOnline.startMatchedGames(java.time.Instant.now()).runningGames

    assertEquals(matchedGames.size, 1)
  }

  test("Two games started when four players looking for two 1vs1 games") {

    val matchedGames = fourPlayersLookingForTwoGames
      .startMatchedGames(java.time.Instant.now())
      .runningGames
    assertEquals(matchedGames.size, 2)
  }

  test("Two players are still waiting for a game") {
    val state =
      sixPlayersLookingForAGame.startMatchedGames(java.time.Instant.now())
    assertEquals(state.runningGames.size, 2)
    assertEquals(state.lookingForGame.size, 2)
  }

  test("Three games started with one TwoVsTwo game") {
    val matchedGames = eightPlayersWithOneTwoVsTwoGameSearch
      .startMatchedGames(java.time.Instant.now())
      .runningGames
    assertEquals(matchedGames.size, 3)
    assertEquals(matchedGames.count(_.gameType == GameType.TwoVsTwo), 1)
  }

}
