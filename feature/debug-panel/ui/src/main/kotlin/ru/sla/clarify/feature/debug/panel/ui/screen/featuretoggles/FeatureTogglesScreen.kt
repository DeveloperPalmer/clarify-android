package ru.sla.clarify.feature.debug.panel.ui.screen.featuretoggles

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.kode.amvi.component.compose.rememberViewIntents
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.uikit.component.Divider
import ru.sla.clarify.uikit.component.topappbar.TopAppBar
import ru.sla.clarify.uikit.component.topappbar.TopAppBarDefaults
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun FeatureTogglesScreen(viewModel: FeatureTogglesViewModel) {
  MviComponent(
    viewModel = viewModel,
    intents = rememberViewIntents()
  ) { state, intents ->
    BackHandler(onBack = intents.navigateBack)
    ScreenScaffold {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .systemBarsPadding()
      ) {
        TopAppBar(
          navigationIcon = { TopAppBarDefaults.NavigationIcon(intents.navigateBack) },
          title = {
            Text(
              text = stringResource(R.string.debug_panel_feature_toggles),
              style = AppTheme.typography.title2Bold,
              color = AppTheme.colors.contentPrimary
            )
          }
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
          items(
            items = state.featureToggles,
            key = { it.feature.key }
          ) { toggle ->
            FeatureToggleItem(
              toggle = toggle,
              onCheckedChange = { isEnabled ->
                intents.changeFeatureToggle(toggle.feature to isEnabled)
              }
            )
            Divider()
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureToggleItem(
  toggle: FeatureToggle,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        horizontal = 16.dp,
        vertical = 12.dp
      ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        text = toggle.feature.key,
        style = AppTheme.typography.body1,
        color = AppTheme.colors.contentPrimary
      )
      if (toggle.description.isNotBlank()) {
        Text(
          text = toggle.description,
          style = AppTheme.typography.body3,
          color = AppTheme.colors.contentSecondary
        )
      }
    }
    Switch(
      checked = toggle.isEnabled,
      onCheckedChange = onCheckedChange
    )
  }
}
