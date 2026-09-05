package ru.sla.clarify.app.data

import android.content.Context
import ru.kode.pathfinder.Configuration
import ru.kode.pathfinder.PathFinder
import ru.kode.pathfinder.android.store.SqlDelightStore
import ru.sla.clarify.app.domain.buildconfig.BuildType

suspend fun createPathfinder(context: Context, buildType: BuildType): PathFinder {
  return PathFinder.create(
    SqlDelightStore(context),
    Configuration(
      environments = buildServerEnvironments(buildType),
      urlSpecs = buildUrlSpecList(),
      defaultEnvironmentId = getDefaultEnvironmentId(buildType)
    )
  )
}
