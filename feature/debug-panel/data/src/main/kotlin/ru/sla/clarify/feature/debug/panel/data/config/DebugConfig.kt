package ru.sla.clarify.feature.debug.panel.data.config

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import ru.kode.plexus.core.Config
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.ApplicationContext
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
class DebugConfig @Inject constructor(@ApplicationContext context: Context) : Config {

  private val preferences: SharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  private val features = MutableStateFlow(readAll())

  override fun getValueSync(key: String): String? {
    return features.value[key]
  }

  override fun getValue(key: String): Flow<String?> {
    return features.map { it[key] }
  }

  override fun getValues(keys: List<String>): Flow<Map<String, String?>> {
    return features.map { values -> keys.associateWith { values[it] } }
  }

  fun setFeatureValue(key: String, value: String) {
    preferences.edit().putString(key, value).apply()
    features.value = readAll()
  }

  private fun readAll(): Map<String, String> {
    // SharedPreferences.all отдаёт Map<String, Any?>: у null-значения toString() дал бы строку
    // "null", и тумблер получил бы её как значение. Такие ключи просто не считаем заданными.
    return preferences.all.mapNotNull { (key, value) ->
      value?.let { key to it.toString() }
    }.toMap()
  }
}

private const val PREFERENCES_NAME = "debug_feature_toggles"
