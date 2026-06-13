package ru.sla.clarify.feature.chat.direct.thread.routing.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import ru.sla.clarify.feature.chat.direct.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.ThreadTarget
import ru.sla.clarify.feature.entity.chat.Peer

@Module
@ContributesTo(ThreadScope::class)
object ThreadTargetModule {

  // Peer.Id запрашивают только direct-классы (ThreadRepositoryImpl, ThreadMediator);
  // в групповом флоу они не инстанцируются, поэтому error до них не доходит.
  @Provides
  fun providePeerId(target: ThreadTarget): Peer.Id {
    return when (target) {
      is ThreadTarget.Direct -> target.peerId
      is ThreadTarget.Group -> error("Peer.Id is not available in a group thread")
    }
  }
}
