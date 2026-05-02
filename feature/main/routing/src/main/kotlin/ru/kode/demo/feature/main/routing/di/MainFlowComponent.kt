package ru.kode.demo.feature.main.routing.di

import com.squareup.anvil.annotations.MergeSubcomponent
import ru.kode.demo.core.domain.di.scope.SingleIn
import ru.kode.demo.feature.main.domain.di.MainScope

@MergeSubcomponent(MainScope::class)
@SingleIn(MainScope::class)
interface MainFlowComponent {
  fun nodeFactory(): MainFlowNodeFactory
}
