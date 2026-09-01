package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable

@Immutable
enum class GraphLevel {
  /** Эпизоды: базовый уровень, плашка-кластер (§6.2 брифа). */
  LOD0,

  /** Обзор: глиф вместо плашки, зазоры и дорожки вчетверо теснее. */
  LOD1
}
