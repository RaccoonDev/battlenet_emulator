package com.devraccoon.starcraft.domain

import io.estatico.newtype.macros.newtype

import java.util.UUID

object player {
  @newtype case class PlayerId(value: UUID)
  object PlayerId {
    def newRandom: PlayerId = PlayerId(UUID.randomUUID())
  }

  @newtype case class Nickname(value: String)

  final case class PlayerRegistered(id: PlayerId,
                                    registrationTime: java.time.Instant,
                                    nickname: Nickname)
}
