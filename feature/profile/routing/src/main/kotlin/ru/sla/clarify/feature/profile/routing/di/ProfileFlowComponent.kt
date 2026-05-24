package ru.sla.clarify.feature.profile.routing.di

import com.squareup.anvil.annotations.MergeSubcomponent
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.profile.domain.di.ProfileScope

@MergeSubcomponent(ProfileScope::class)
@SingleIn(ProfileScope::class)
interface ProfileFlowComponent {
  fun nodeFactory(): ProfileFlowNodeFactory
}
