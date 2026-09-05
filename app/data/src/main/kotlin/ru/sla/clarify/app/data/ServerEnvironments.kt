package ru.sla.clarify.app.data

import ru.kode.pathfinder.Environment
import ru.kode.pathfinder.EnvironmentId
import ru.sla.clarify.app.domain.buildconfig.BuildType

/**
 * Стенды, между которыми переключается сборка.
 *
 * Адреса ещё не существуют: сервер не поднят. До тех пор здесь стоит `.invalid` —
 * зарезервированный RFC 2606 домен, который заведомо не резолвится, поэтому забытый плейсхолдер
 * падает на DNS, а не уходит молча в чужой хост, случайно занявший это имя.
 */
internal fun buildServerEnvironments(buildType: BuildType): List<Environment> {
  return when (buildType) {
    BuildType.Dev,
    BuildType.Internal -> {
      listOf(
        Environment(
          id = EnvironmentId("dev"),
          name = "Dev",
          baseUrl = "https://dev-api.clarify.invalid"
        ),
        Environment(
          id = EnvironmentId("stage"),
          name = "Stage",
          baseUrl = "https://stage-api.clarify.invalid"
        ),
        Environment(
          id = EnvironmentId("prod"),
          name = "Production",
          baseUrl = "https://api.clarify.invalid"
        )
      )
    }

    BuildType.Release -> {
      listOf(
        Environment(
          id = EnvironmentId("prod"),
          name = "Production",
          baseUrl = "https://api.clarify.invalid"
        )
      )
    }
  }
}

/**
 * Стенд, на котором сборка стартует. Отладочные сборки идут на stage, а не на dev: dev ломается
 * чаще всех, и стартовать на нём значит объяснять каждое утро, что сломан стенд, а не клиент.
 */
internal fun getDefaultEnvironmentId(buildType: BuildType): EnvironmentId {
  return when (buildType) {
    BuildType.Dev,
    BuildType.Internal -> EnvironmentId("stage")

    BuildType.Release -> EnvironmentId("prod")
  }
}
