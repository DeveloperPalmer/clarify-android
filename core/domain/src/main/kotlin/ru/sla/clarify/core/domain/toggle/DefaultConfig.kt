package ru.sla.clarify.core.domain.toggle

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ru.kode.plexus.core.Config

class DefaultConfig : Config {

  override fun getValueSync(key: String): String? {
    return features[key]
  }

  override fun getValue(key: String): Flow<String?> {
    return flowOf(features[key])
  }

  override fun getValues(keys: List<String>): Flow<Map<String, String?>> {
    return flowOf(keys.associateWith { features[it] })
  }

  private val features: Map<String, String> = AppFeature.entries.associate { feature ->
    feature.key to when (feature) {
      AppFeature.GroupsAvailable -> "false"
    }
  }
}
