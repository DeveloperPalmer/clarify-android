package ru.sla.clarify.uikit.component.topappbar

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.component.icon.IconAction
import ru.sla.clarify.uikit.theme.AppTheme
import androidx.compose.material3.TopAppBar as MaterialTopAppBar
import androidx.compose.material3.TopAppBarDefaults as MaterialTopAppBarDefaults

@Composable
fun TopAppBar(
  modifier: Modifier = Modifier,
  title: @Composable () -> Unit,
  navigationIcon: @Composable () -> Unit = { },
  actions: @Composable RowScope.() -> Unit = { }
) {
  MaterialTopAppBar(
    modifier = modifier,
    title = title,
    navigationIcon = navigationIcon,
    actions = actions,
    windowInsets = MaterialTopAppBarDefaults.windowInsets,
    colors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior = null
  )
}

object TopAppBarDefaults {

  @Composable
  fun NavigationIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
  ) {
    IconAction(
      modifier = modifier,
      iconResId = R.drawable.ic_chevron_left_24,
      onClick = onClick
    )
  }

  @Composable
  fun topAppBarColors(): TopAppBarColors {
    return MaterialTopAppBarDefaults.topAppBarColors(
      containerColor = AppTheme.colors.backgroundPrimary,
      scrolledContainerColor = AppTheme.colors.backgroundPrimary,
      navigationIconContentColor = AppTheme.colors.contentPrimary,
      titleContentColor = AppTheme.colors.contentPrimary,
      actionIconContentColor = AppTheme.colors.contentPrimary
    )
  }
}

@Composable
fun rememberTopBarElevation(listState: LazyListState): State<Dp> {
  val isScrolledUnderTopBar by remember {
    derivedStateOf { listState.canScrollForward }
  }
  return animateDpAsState(
    label = "topBarElevation",
    targetValue = if (isScrolledUnderTopBar) 4.dp else 0.dp
  )
}
