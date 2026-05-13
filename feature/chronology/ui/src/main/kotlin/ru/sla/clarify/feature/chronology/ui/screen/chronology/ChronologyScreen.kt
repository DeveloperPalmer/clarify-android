package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.kode.amvi.component.compose.MviComponent
import ru.kode.amvi.component.compose.rememberViewIntents
import ru.sla.clarify.feature.chronology.domain.ChronologyGraph
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun ChronologyScreen(viewModel: ChronologyViewModel) {
  MviComponent(
    viewModel = viewModel,
    intents = rememberViewIntents()
  ) { state, intents ->
    val scaffoldState = rememberScreenScaffoldState()
    scaffoldState.contentLoadState = state.contentLoadState
    ScreenScaffold(state = scaffoldState) {
      ChronologyReadyContent(
        peerId = state.peerId,
        graph = state.graph,
        onBack = intents.navigateBack
      )
    }
  }
}

@Composable
internal fun ChronologyReadyContent(
  peerId: String,
  graph: ChronologyGraph,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxSize()) {
    ChronologyTopBar(
      peerId = peerId,
      onBack = onBack
    )
    ChronologyGraphCanvas(
      graph = graph,
      modifier = Modifier.fillMaxSize()
    )
  }
}

@Composable
private fun ChronologyTopBar(
  peerId: String,
  onBack: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(horizontal = 8.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    IconButton(onClick = onBack) {
      Text(
        text = "<",
        style = AppTheme.typography.h2
      )
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "Хронология",
        style = AppTheme.typography.title1,
        fontWeight = FontWeight.SemiBold
      )
      Text(
        text = peerId,
        style = AppTheme.typography.caption2
      )
    }
  }
  HorizontalDivider()
}
