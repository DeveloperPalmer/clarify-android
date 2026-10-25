package ru.sla.clarify.feature.debug.panel.data.di

import dagger.Binds
import dagger.Module
import ru.sla.clarify.feature.debug.panel.data.DebugPanelRepositoryImpl
import ru.sla.clarify.feature.debug.panel.domain.DebugPanelRepository

@Module
interface DebugPanelDataModule {
  @Binds
  fun bindDebugPanelRepository(impl: DebugPanelRepositoryImpl): DebugPanelRepository
}
