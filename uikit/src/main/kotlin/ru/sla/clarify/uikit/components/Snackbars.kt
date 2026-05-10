package ru.sla.clarify.uikit.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.runtime.Composable
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun ErrorSnackbar(data: SnackbarData) {
  Snackbar(
    snackbarData = data,
    shape = AppTheme.shapes.round24,
    containerColor = AppTheme.colors.errorPrimary,
    contentColor = AppTheme.colors.textPrimary,
    actionColor = AppTheme.colors.textPrimary
  )
}

@Composable
fun MessageSnackbar(data: SnackbarData) {
  Snackbar(
    snackbarData = data,
    shape = AppTheme.shapes.round24,
    containerColor = AppTheme.colors.buttonPrimary,
    contentColor = AppTheme.colors.textPrimary,
    actionColor = AppTheme.colors.textPrimary
  )
}
