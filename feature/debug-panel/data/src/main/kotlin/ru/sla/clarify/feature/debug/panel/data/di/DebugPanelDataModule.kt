package ru.sla.clarify.feature.debug.panel.data.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import ru.sla.clarify.feature.debug.panel.data.DebugPanelRepositoryImpl
import ru.sla.clarify.feature.debug.panel.domain.DebugPanelRepository
import ru.sla.clarify.feature.debug.panel.domain.di.DebugPanelScope

@Module
@ContributesTo(DebugPanelScope::class)
interface DebugPanelDataModule {
  @Binds
  fun bindDebugPanelRepository(impl: DebugPanelRepositoryImpl): DebugPanelRepository
}
