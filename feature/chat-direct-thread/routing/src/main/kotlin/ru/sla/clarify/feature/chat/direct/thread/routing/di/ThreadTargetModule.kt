package ru.sla.clarify.feature.chat.direct.thread.routing.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import ru.sla.clarify.feature.chat.direct.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.TargetParams
import ru.sla.clarify.feature.entity.chat.Peer

@Module
@ContributesTo(ThreadScope::class)
object ThreadTargetModule {

  @Provides
  fun providePeerId(target: TargetParams): Peer.Id {
    return target.peerId
  }
}
