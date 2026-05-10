package ru.sla.clarify.feature.chronology.data

import com.squareup.anvil.annotations.ContributesBinding
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chronology.domain.ChronologyRepository
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope
import javax.inject.Inject

@SingleIn(ChronologyScope::class)
@ContributesBinding(ChronologyScope::class)
class ChronologyRepositoryImpl @Inject constructor() : ChronologyRepository
