package ru.sla.clarify.feature.chat.branch.data.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import ru.sla.clarify.feature.chat.branch.data.BranchRepositoryImpl
import ru.sla.clarify.feature.chat.branch.domain.BranchRepository
import ru.sla.clarify.feature.chat.branch.domain.di.BranchScope

@Module
@ContributesTo(BranchScope::class)
interface BranchDataModule {
  @Binds
  fun bindBranchRepository(impl: BranchRepositoryImpl): BranchRepository
}
