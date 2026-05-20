package ru.sla.clarify.uikit.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

@Composable
fun IconAction(
  iconResId: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  contentDescription: String? = null
) {
  IconButton(
    modifier = modifier,
    onClick = onClick
  ) {
    Icon(
      painter = painterResource(iconResId),
      contentDescription = contentDescription
    )
  }
}
