package ru.sla.clarify.feature.chat.group.thread.ui.screen.invitemembers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chat.group.thread.ui.entity.InviteCandidate
import ru.sla.clarify.uikit.component.avatar.Avatar
import ru.sla.clarify.uikit.component.button.PrimaryButton
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

@Composable
fun InviteMembersContent(
  query: String,
  candidates: List<InviteCandidate>,
  selected: List<InviteCandidate>,
  isLimitReached: Boolean,
  onQueryChange: (String) -> Unit,
  onClose: () -> Unit,
  onToggleCandidate: (InviteCandidate) -> Unit,
  onConfirm: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxSize()) {
    Header(onClose = onClose)
    SearchField(
      modifier = Modifier.padding(horizontal = 16.dp),
      query = query,
      onQueryChange = onQueryChange
    )
    if (isLimitReached) {
      LimitHint(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
    Box(modifier = Modifier.weight(1f)) {
      Results(
        candidates = candidates,
        selected = selected,
        isLimitReached = isLimitReached,
        onToggleCandidate = onToggleCandidate
      )
    }
    ConfirmBar(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      selectedCount = selected.size,
      onConfirm = onConfirm
    )
  }
}

@Composable
private fun Header(onClose: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      modifier = Modifier.weight(1f),
      text = stringResource(R.string.invite_members_title),
      style = AppTheme.typography.title1Bold,
      color = AppTheme.colors.contentPrimary
    )
    Icon(
      modifier = Modifier
        .size(24.dp)
        .clickable(onClick = onClose),
      painter = painterResource(R.drawable.ic_close_24),
      tint = AppTheme.colors.contentSecondary,
      contentDescription = stringResource(R.string.action_cancel)
    )
  }
}

@Composable
private fun SearchField(
  query: String,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(48.dp)
      .clip(AppTheme.shapes.round16)
      .background(AppTheme.colors.cardSecondary)
      .padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Icon(
      modifier = Modifier.size(20.dp),
      painter = painterResource(R.drawable.ic_search_24),
      tint = AppTheme.colors.contentSecondary,
      contentDescription = null
    )
    Box(modifier = Modifier.weight(1f)) {
      if (query.isEmpty()) {
        Text(
          text = stringResource(R.string.invite_members_search_placeholder),
          style = AppTheme.typography.body2,
          color = AppTheme.colors.contentSecondary
        )
      }
      BasicTextField(
        modifier = Modifier.fillMaxWidth(),
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        cursorBrush = SolidColor(AppTheme.colors.contentAccentPrimary),
        textStyle = AppTheme.typography.body2.copy(color = AppTheme.colors.contentPrimary)
      )
    }
  }
}

@Composable
private fun LimitHint(modifier: Modifier = Modifier) {
  Text(
    modifier = modifier,
    text = stringResource(R.string.invite_members_limit_reached),
    style = AppTheme.typography.label3,
    color = AppTheme.colors.contentSecondary
  )
}

@Composable
private fun Results(
  candidates: List<InviteCandidate>,
  selected: List<InviteCandidate>,
  isLimitReached: Boolean,
  onToggleCandidate: (InviteCandidate) -> Unit
) {
  if (candidates.isEmpty() && selected.isEmpty()) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text(
        modifier = Modifier.padding(horizontal = 32.dp),
        text = stringResource(R.string.invite_members_empty),
        style = AppTheme.typography.body2,
        color = AppTheme.colors.contentSecondary,
        textAlign = TextAlign.Center
      )
    }
    return
  }
  LazyColumn(modifier = Modifier.fillMaxSize()) {
    if (selected.isNotEmpty()) {
      items(
        count = selected.size,
        key = { "selected:${selected[it].id}" }
      ) { index ->
        CandidateRow(
          candidate = selected[index],
          isSelectionDisabled = false,
          onToggle = { onToggleCandidate(selected[index]) }
        )
      }
    }
    items(
      count = candidates.size,
      key = { "result:${candidates[it].id}" }
    ) { index ->
      val candidate = candidates[index]
      val selectionDisabled = !candidate.isSelected &&
        (candidate.isAlreadyMember || isLimitReached)
      CandidateRow(
        candidate = candidate,
        isSelectionDisabled = selectionDisabled,
        onToggle = { onToggleCandidate(candidate) }
      )
    }
  }
}

@Composable
private fun CandidateRow(
  candidate: InviteCandidate,
  isSelectionDisabled: Boolean,
  onToggle: () -> Unit
) {
  val rowAlpha = if (candidate.isAlreadyMember) 0.5f else 1f
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .alpha(rowAlpha)
      .let { if (isSelectionDisabled) it else it.clickable(onClick = onToggle) }
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Avatar(
      size = 40.dp,
      photoUrl = candidate.photoUrl,
      fallbackInitial = candidate.displayName
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = candidate.displayName,
        style = AppTheme.typography.body1,
        color = AppTheme.colors.contentPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = candidate.email,
        style = AppTheme.typography.label3,
        color = AppTheme.colors.contentSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    if (candidate.isAlreadyMember) {
      Text(
        text = stringResource(R.string.invite_members_already_member),
        style = AppTheme.typography.label3,
        color = AppTheme.colors.contentSecondary
      )
    } else {
      Checkbox(checked = candidate.isSelected)
    }
  }
}

@Composable
private fun Checkbox(checked: Boolean) {
  Box(
    modifier = Modifier
      .size(22.dp)
      .clip(CircleShape)
      .then(
        if (checked) {
          Modifier.background(AppTheme.colors.contentAccentPrimary)
        } else {
          Modifier.border(1.5.dp, AppTheme.colors.cardQuinary, CircleShape)
        }
      ),
    contentAlignment = Alignment.Center
  ) {
    if (checked) {
      Icon(
        modifier = Modifier.size(14.dp),
        painter = painterResource(R.drawable.ic_check_16),
        tint = AppTheme.colors.contentAccentSecondary,
        contentDescription = null
      )
    }
  }
}

@Composable
private fun ConfirmBar(
  selectedCount: Int,
  onConfirm: () -> Unit,
  modifier: Modifier = Modifier
) {
  PrimaryButton(
    modifier = modifier,
    text = stringResource(R.string.invite_members_confirm_button, selectedCount),
    enabled = selectedCount > 0,
    onClick = onConfirm
  )
}

@Preview
@Composable
private fun InviteMembersPreviewEmptyQuery() {
  PreviewColumn {
    InviteMembersContent(
      modifier = Modifier.fillMaxSize(),
      query = "",
      candidates = emptyList(),
      selected = emptyList(),
      isLimitReached = false,
      onQueryChange = {},
      onClose = {},
      onToggleCandidate = {},
      onConfirm = {}
    )
  }
}

@Preview
@Composable
private fun InviteMembersPreviewResultsLight() {
  PreviewColumn {
    InviteMembersContent(
      modifier = Modifier.fillMaxSize(),
      query = "ann",
      candidates = sampleCandidates(),
      selected = emptyList(),
      isLimitReached = false,
      onQueryChange = {},
      onClose = {},
      onToggleCandidate = {},
      onConfirm = {}
    )
  }
}

@Preview
@Composable
private fun InviteMembersPreviewSelectedDark() {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    val all = sampleCandidates()
    InviteMembersContent(
      modifier = Modifier.fillMaxSize(),
      query = "ann",
      candidates = all.drop(1),
      selected = listOf(all.first().copy(isSelected = true)),
      isLimitReached = false,
      onQueryChange = {},
      onClose = {},
      onToggleCandidate = {},
      onConfirm = {}
    )
  }
}

@Preview
@Composable
private fun InviteMembersPreviewLimitReached() {
  PreviewColumn {
    InviteMembersContent(
      modifier = Modifier.fillMaxSize(),
      query = "i",
      candidates = sampleCandidates(),
      selected = sampleCandidates().map { it.copy(isSelected = true) },
      isLimitReached = true,
      onQueryChange = {},
      onClose = {},
      onToggleCandidate = {},
      onConfirm = {}
    )
  }
}

@Preview
@Composable
private fun InviteMembersPreviewEmpty() {
  PreviewColumn {
    InviteMembersContent(
      modifier = Modifier.fillMaxSize(),
      query = "zzz",
      candidates = emptyList(),
      selected = emptyList(),
      isLimitReached = false,
      onQueryChange = {},
      onClose = {},
      onToggleCandidate = {},
      onConfirm = {}
    )
  }
}

private fun sampleCandidates(): List<InviteCandidate> = listOf(
  InviteCandidate(
    id = "u1",
    displayName = "Аня Котова",
    email = "anna@example.com",
    photoUrl = null,
    isAlreadyMember = false,
    isSelected = false
  ),
  InviteCandidate(
    id = "u2",
    displayName = "Илья Соколов",
    email = "ilya@example.com",
    photoUrl = null,
    isAlreadyMember = true,
    isSelected = false
  ),
  InviteCandidate(
    id = "u3",
    displayName = "Мария García",
    email = "maria@example.com",
    photoUrl = null,
    isAlreadyMember = false,
    isSelected = false
  )
)
