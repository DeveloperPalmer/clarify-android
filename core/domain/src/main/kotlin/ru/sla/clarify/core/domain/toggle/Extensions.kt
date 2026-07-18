package ru.sla.clarify.core.domain.toggle

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.kode.plexus.core.FeatureConfigsManager

fun FeatureConfigsManager.isFeatureEnabledLive(feature: AppFeature): Flow<Boolean> {
  return getFeatureValue(feature.key).map { it.toBoolean() }
}

fun FeatureConfigsManager.isFeatureEnabled(feature: AppFeature): Boolean {
  return getFeatureValueSync(feature.key).toBoolean()
}
