package ru.sla.clarify.feature.chat.branch.data.di

import dagger.Binds
import dagger.Module
import ru.sla.clarify.feature.chat.branch.data.BranchRepositoryImpl
import ru.sla.clarify.feature.chat.branch.domain.BranchRepository

@Module
interface BranchDataModule {
  @Binds
  fun bindBranchRepository(impl: BranchRepositoryImpl): BranchRepository
}
