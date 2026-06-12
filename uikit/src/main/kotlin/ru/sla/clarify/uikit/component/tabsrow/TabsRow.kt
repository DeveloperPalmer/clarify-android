package ru.sla.clarify.uikit.component.tabsrow

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.resourcerefs.compose.resolveTextRef

@Composable
internal fun <T : Tab> TabsRowInternal(
  tabList: List<T>,
  selectedTab: T,
  onTabClick: (T) -> Unit,
  height: Dp,
  borderWidth: Dp?,
  colors: TabsRowColors,
  modifier: Modifier = Modifier
) {
  BasicTabsRow(
    modifier = modifier,
    height = height,
    colors = colors,
    borderWidth = borderWidth,
    indicatorPadding = PaddingValues(3.dp),
    selectedTabIndex = remember(selectedTab) { tabList.indexOf(selectedTab) },
    tabs = {
      tabList.forEach { tab ->
        Text(
          modifier = Modifier
            .zIndex(1f)
            .wrapContentSize()
            .clickable(
              indication = null,
              interactionSource = null,
              onClick = { onTabClick(tab) }
            )
            .padding(PaddingValues(10.dp)),
          text = resolveTextRef(tab.title),
          style = AppTheme.typography.body3Bold,
          textAlign = TextAlign.Center,
          color = if (tab == selectedTab) {
            colors.selectedColor().value
          } else {
            colors.unselectedColor().value
          }
        )
      }
    }
  )
}

@Composable
private fun BasicTabsRow(
  selectedTabIndex: Int,
  height: Dp,
  colors: TabsRowColors,
  indicatorPadding: PaddingValues,
  modifier: Modifier = Modifier,
  borderWidth: Dp? = null,
  shape: Shape = AppTheme.shapes.round16,
  indicator: @Composable (tabPositions: List<TabPosition>) -> Unit = @Composable { tabPositions ->
    if (selectedTabIndex < tabPositions.size) {
      Box(
        modifier = Modifier
          .tabIndicatorOffset(tabPositions[selectedTabIndex])
          .fillMaxSize()
          .padding(indicatorPadding)
          .surface(
            shape = RoundedCornerShape(14.dp),
            backgroundColor = colors.indicatorColor().value
          )
      )
    }
  },
  tabs: @Composable () -> Unit
) {
  Box(
    modifier = modifier
      .surface(
        shape = shape,
        border = borderWidth?.let { BorderStroke(it, colors.borderColor().value) },
        backgroundColor = colors.backgroundColor().value
      )
      .pointerInput(Unit) {},
    propagateMinConstraints = true
  ) {
    SubcomposeLayout(
      Modifier
        .fillMaxWidth()
        .heightIn(height)
    ) { constraints ->
      val tabRowWidth = constraints.maxWidth
      val tabMeasurables = subcompose(TabSlots.Tabs, tabs)
      val tabCount = tabMeasurables.size
      var tabWidth = 0
      if (tabCount > 0) {
        tabWidth = (tabRowWidth / tabCount)
      }
      val tabRowHeight = tabMeasurables.fold(initial = 0) { max, curr ->
        val containerHeight = maxOf(height.roundToPx(), max)
        val currentTabHeight = curr.maxIntrinsicHeight(tabWidth)
        maxOf(currentTabHeight, containerHeight)
      }

      val tabPlaceables = tabMeasurables.map {
        it.measure(
          constraints.copy(
            minWidth = tabWidth,
            maxWidth = tabWidth,
            minHeight = tabRowHeight,
            maxHeight = tabRowHeight
          )
        )
      }

      val tabPositions = List(tabCount) { index ->
        TabPosition(tabWidth.toDp() * index, tabWidth.toDp())
      }

      layout(tabRowWidth, tabRowHeight) {
        tabPlaceables.forEachIndexed { index, placeable ->
          placeable.placeRelative(index * tabWidth, 0)
        }

        subcompose(TabSlots.Indicator) {
          indicator(tabPositions)
        }.forEach {
          it.measure(Constraints.fixed(tabRowWidth, tabRowHeight)).placeRelative(0, 0)
        }
      }
    }
  }
}

/**
 * [Modifier] that takes up all the available width inside the [PrimaryTabsRow], and then animates
 * the offset of the indicator it is applied to, depending on the [currentTabPosition].
 *
 * @param currentTabPosition [TabPosition] of the currently selected tab. This is used to
 * calculate the offset of the indicator this modifier is applied to, as well as its width.
 */
private fun Modifier.tabIndicatorOffset(currentTabPosition: TabPosition): Modifier {
  return composed(
    inspectorInfo = debugInspectorInfo {
      name = "tabIndicatorOffset"
      value = currentTabPosition
    },
    factory = {
      val currentTabWidth by animateDpAsState(
        label = "currentTabWidthLabel",
        targetValue = currentTabPosition.width,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
      )
      val indicatorOffset by animateDpAsState(
        label = "indicatorOffsetLabel",
        targetValue = currentTabPosition.left,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
      )
      fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = indicatorOffset)
        .width(currentTabWidth)
    }
  )
}

object DefaultTabsRow {

  @Composable
  fun primaryColors(
    borderColor: Color = Color.Unspecified,
    indicatorColor: Color = AppTheme.colors.cardPrimary,
    backgroundColor: Color = AppTheme.colors.cardSecondary,
    selectedColor: Color = AppTheme.colors.contentPrimary,
    unselectedColor: Color = AppTheme.colors.contentTertiary
  ): DefaultTabsRowColors {
    return DefaultTabsRowColors(
      borderColor,
      indicatorColor,
      backgroundColor,
      selectedColor,
      unselectedColor
    )
  }
}

/**
 * Data class that contains information about a tab's position on screen, used for calculating
 * where to place the indicator that shows which tab is selected.
 *
 * @property left the left edge's x position from the start of the [PrimaryTabsRow]
 * @property right the right edge's x position from the start of the [PrimaryTabsRow]
 * @property width the width of this tab
 */
@Immutable
data class TabPosition internal constructor(val left: Dp, val width: Dp) {
  val right: Dp get() = left + width

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TabPosition) return false

    if (left != other.left) return false
    if (width != other.width) return false

    return true
  }

  override fun hashCode(): Int {
    var result = left.hashCode()
    result = 31 * result + width.hashCode()
    return result
  }
}

@Stable
interface TabsRowColors {
  @Composable
  fun borderColor(): State<Color>

  @Composable
  fun indicatorColor(): State<Color>

  @Composable
  fun backgroundColor(): State<Color>

  @Composable
  fun selectedColor(): State<Color>

  @Composable
  fun unselectedColor(): State<Color>
}

@Immutable
class DefaultTabsRowColors(
  private val borderColor: Color,
  private val indicatorColor: Color,
  private val backgroundColor: Color,
  private val selectedColor: Color,
  private val unselectedColor: Color
) : TabsRowColors {

  @Composable
  override fun borderColor(): State<Color> {
    return rememberUpdatedState(borderColor)
  }

  @Composable
  override fun indicatorColor(): State<Color> {
    return rememberUpdatedState(indicatorColor)
  }

  @Composable
  override fun backgroundColor(): State<Color> {
    return rememberUpdatedState(backgroundColor)
  }

  @Composable
  override fun selectedColor(): State<Color> {
    return rememberUpdatedState(selectedColor)
  }

  @Composable
  override fun unselectedColor(): State<Color> {
    return rememberUpdatedState(unselectedColor)
  }
}

private enum class TabSlots {
  Tabs,
  Indicator
}
