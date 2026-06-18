package ru.sla.clarify.uikit.component.bottomsheet

import android.view.View
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.window.setNavigationBarColorCompat

@Composable
fun ModalBottomSheet(
  visible: Boolean,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  useDefaultHandle: Boolean = true,
  sheetGesturesEnabled: Boolean = true,
  onUpdateState: ((SheetState) -> Unit)? = null,
  verticalArrangement: Arrangement.Vertical = Arrangement.Top,
  sheetBackgroundColor: Color = AppTheme.colors.backgroundPrimary,
  properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
  sheetContent: @Composable ColumnScope.() -> Unit
) {
  if (visible) {
    LaunchedEffect(Unit) {
      if (onUpdateState != null) {
        onUpdateState(sheetState)
      }
    }
    val isLightTheme = AppTheme.colors.isLight
    ModalBottomSheet(
      modifier = modifier.statusBarsPadding(),
      sheetState = sheetState,
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      dragHandle = {},
      containerColor = sheetBackgroundColor,
      sheetGesturesEnabled = sheetGesturesEnabled,
      tonalElevation = SheetDefaultElevation,
      scrimColor = Color.Black.copy(0.4f),
      contentWindowInsets = {
        WindowInsets.safeDrawing
          .only(WindowInsetsSides.Top)
          .union(WindowInsets.ime)
      },
      properties = ModalBottomSheetProperties(
        isAppearanceLightStatusBars = isLightTheme,
        isAppearanceLightNavigationBars = isLightTheme,
        securePolicy = properties.securePolicy,
        shouldDismissOnBackPress = properties.shouldDismissOnBackPress,
        shouldDismissOnClickOutside = properties.shouldDismissOnClickOutside
      ),
      onDismissRequest = { onDismissRequest() }
    ) {
      val sheetView = LocalView.current
      SideEffect {
        findDialogWindow(sheetView)?.setNavigationBarColorCompat(
          color = Color.Transparent.toArgb(),
          contrastEnforced = false
        )
      }
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding(),
        verticalArrangement = verticalArrangement
      ) {
        if (useDefaultHandle) {
          BottomSheetHandle()
        }
        sheetContent()
      }
    }
  }
}

@Composable
private fun BottomSheetHandle(
  modifier: Modifier = Modifier,
  handleColor: Color = AppTheme.colors.contentPrimary
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Spacer(
      modifier = Modifier
        .size(
          width = 56.dp,
          height = 4.dp
        )
        .background(
          color = handleColor,
          shape = RoundedCornerShape(5.dp)
        )
    )
  }
}

private tailrec fun findDialogWindow(view: View?): Window? {
  return when (val parent = view?.parent) {
    is DialogWindowProvider -> parent.window
    is View -> findDialogWindow(parent)
    else -> null
  }
}

private val SheetDefaultElevation = 16.dp
