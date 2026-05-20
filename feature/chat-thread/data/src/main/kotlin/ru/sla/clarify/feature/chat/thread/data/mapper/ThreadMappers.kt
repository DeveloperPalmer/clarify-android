package ru.sla.clarify.feature.chat.thread.data.mapper

import kotlin.random.Random

internal fun generateColorHex(): String {
  val rgb = Random.nextInt(0x1000000)
  return "#$OPAQUE_ALPHA_HEX${rgb.toString(radix = 16).padStart(6, '0').uppercase()}"
}

private const val OPAQUE_ALPHA_HEX = "FF"
