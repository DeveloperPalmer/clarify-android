package ru.sla.clarify.feature.chronology.data.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import ru.sla.clarify.feature.chronology.data.ChronologyRepositoryImpl
import ru.sla.clarify.feature.chronology.domain.ChronologyRepository
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope

@Module
@ContributesTo(ChronologyScope::class)
interface ChronologyDataModule {
  @Binds
  fun bindChronologyRepository(impl: ChronologyRepositoryImpl): ChronologyRepository
}
