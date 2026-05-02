package ru.sla.clarify.core.ui.text

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun TextFieldValue.take(count: Int): TextFieldValue {
  if (count >= this.text.length) return this
  return TextFieldValue(
    text = this.text.take(count),
    selection = this.selection.updateForLengthAtMost(count),
    composition = this.composition?.updateForLengthAtMost(count)
  )
}

private fun TextRange.updateForLengthAtMost(maximumValue: Int): TextRange {
  return TextRange(start = minOf(this.start, maximumValue), end = minOf(this.end, maximumValue))
}
