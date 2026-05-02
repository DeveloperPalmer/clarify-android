package ru.kode.resourcerefs.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ru.kode.resourcerefs.TextRef

@Suppress("SpreadOperator")
// is not usually called on a performance-critical path
@Composable
@ReadOnlyComposable
fun resolveTextRef(source: TextRef): String {
  return when (source) {
    is TextRef.Res -> {
      if (source.formatArgs.isEmpty()) {
        stringResource(source.id)
      } else {
        stringResource(source.id, *source.formatArgs.toTypedArray())
      }
    }
    is TextRef.QtyRes -> {
      if (source.formatArgs.isEmpty()) {
        pluralStringResource(source.id, source.quantity)
      } else {
        pluralStringResource(source.id, source.quantity, *source.formatArgs.toTypedArray())
      }
    }
    is TextRef.Str -> source.value.toString()
    is TextRef.Compound -> {
      buildString {
        source.refs.forEach {
          append(resolveTextRef(source = it))
        }
      }
    }
  }
}
