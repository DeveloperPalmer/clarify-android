package ru.sla.clarify.uikit.component.button

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun ChatScrollToBottomButton(
  listState: LazyListState,
  unreadCount: Int,
  modifier: Modifier = Modifier
) {
  val isVisible by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
  AnimatedVisibility(
    modifier = modifier,
    visible = isVisible,
    enter = fadeIn() + scaleIn(),
    exit = fadeOut() + scaleOut()
  ) {
    Box(contentAlignment = Alignment.TopCenter) {
      ChatScrollToBottomButtonInternal(
        modifier = Modifier.padding(top = 8.dp),
        listState = listState
      )
      if (unreadCount > 0) {
        UnreadBadge(
          modifier = Modifier.align(Alignment.TopCenter),
          count = unreadCount
        )
      }
    }
  }
}

@Composable
fun ChatScrollToBottomButtonInternal(
  listState: LazyListState,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  Box(
    modifier = modifier
      .size(44.dp)
      .surface(
        elevation = 4.dp,
        shape = CircleShape,
        backgroundColor = AppTheme.colors.cardSecondary,
        onClick = {
          scope.launch {
            if (listState.firstVisibleItemIndex > SCROLL_TO_BOTTOM_ANIMATE_THRESHOLD) {
              listState.scrollToItem(SCROLL_TO_BOTTOM_ANIMATE_THRESHOLD)
            }
            listState.animateScrollToItem(0)
          }
        }
      ),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      painter = painterResource(R.drawable.ic_chevron_down_24),
      tint = AppTheme.colors.contentSecondary,
      contentDescription = stringResource(R.string.thread_scroll_to_bottom)
    )
  }
}

@Composable
private fun UnreadBadge(
  count: Int,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .sizeIn(minWidth = 18.dp, minHeight = 18.dp)
      .surface(
        shape = CircleShape,
        backgroundColor = AppTheme.colors.contentAccentPrimary
      )
      .padding(horizontal = 5.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = count.toString(),
      style = AppTheme.typography.caption,
      color = AppTheme.colors.contentAccentSecondary
    )
  }
}

private const val SCROLL_TO_BOTTOM_ANIMATE_THRESHOLD = 10
