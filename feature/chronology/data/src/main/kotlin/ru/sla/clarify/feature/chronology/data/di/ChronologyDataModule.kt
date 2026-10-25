package ru.sla.clarify.feature.chronology.data.di

import dagger.Binds
import dagger.Module
import ru.sla.clarify.feature.chronology.data.ChronologyRepositoryImpl
import ru.sla.clarify.feature.chronology.domain.ChronologyRepository

@Module
interface ChronologyDataModule {
  @Binds
  fun bindChronologyRepository(impl: ChronologyRepositoryImpl): ChronologyRepository
}
