package ru.sla.clarify.feature.debug.panel.domain

import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.debug.panel.domain.di.DebugPanelScope
import javax.inject.Inject

@SingleIn(DebugPanelScope::class)
class DebugPanelModel @Inject constructor() : ReactiveModel()
