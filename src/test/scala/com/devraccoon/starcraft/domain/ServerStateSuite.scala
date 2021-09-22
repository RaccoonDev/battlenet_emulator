package com.devraccoon.starcraft.domain

import com.devraccoon.starcraft.domain.game.GameType
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

  private val state =
    State.empty
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
    val fourPlayersLookingForTwoGames = twoPlayersOnline
      .startLookingForGame(java.time.Instant.now(),
                           Set(playerC.id, playerD.id),
                           GameType.OneVsOne)

    val matchedGames = fourPlayersLookingForTwoGames
      .startMatchedGames(java.time.Instant.now())
      .runningGames
    assertEquals(matchedGames.size, 2)
  }

}
