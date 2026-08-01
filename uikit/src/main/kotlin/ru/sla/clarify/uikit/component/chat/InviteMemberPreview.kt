package ru.sla.clarify.uikit.component.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.ColorTheme

@Preview
@Composable
private fun InviteMemberPreviewLight() {
  PreviewColumn {
    InviteMemberPreviewContent()
  }
}

@Preview
@Composable
private fun InviteMemberPreviewDark() {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    InviteMemberPreviewContent()
  }
}

@Composable
private fun InviteMemberPreviewContent() {
  Box(modifier = Modifier.padding(16.dp)) {
    InviteMember(text = "Сергей пригласил(а) Аню Котову")
  }
  Box(modifier = Modifier.padding(16.dp)) {
    InviteMember(
      text = "Сергей пригласил(а) участника с очень длинным именем, " +
        "которое не помещается в одну строку капсулы"
    )
  }
}
