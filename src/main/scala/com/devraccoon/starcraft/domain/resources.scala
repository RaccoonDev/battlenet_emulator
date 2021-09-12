package com.devraccoon.starcraft.domain

import enumeratum.{Enum, EnumEntry}
import io.estatico.newtype.macros.newtype

object resources {
  sealed trait ResourceType extends EnumEntry

  object ResourceType extends Enum[ResourceType] {
    val values: IndexedSeq[ResourceType] = findValues

    case object Minerals extends ResourceType
    case object VespenGas extends ResourceType
  }

  @newtype case class ResourceAmount(value: Long)
  final case class Money(kind: ResourceType, amount: ResourceAmount)
}
