package ru.sla.clarify.feature.chronology.ui.screen.chronology

import me.tatarka.inject.annotations.Inject
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine
import ru.kode.amvi.viewmodel.ViewModel
import ru.sla.clarify.feature.chronology.domain.ChronologyModel

class ChronologyViewModel @Inject constructor(
  private val chronologyModel: ChronologyModel
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState() to null

    onEach(chronologyModel.branches) {
      transitionTo { state, branches -> state.copy(branches = branches) }
    }
  }
}
