package ru.sla.clarify.feature.chronology.domain

import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope
import javax.inject.Inject

@SingleIn(ChronologyScope::class)
class ChronologyModel @Inject constructor() : ReactiveModel()
