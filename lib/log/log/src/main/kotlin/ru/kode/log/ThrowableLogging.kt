package ru.kode.log

import java.io.PrintWriter
import java.io.StringWriter

fun Throwable.asLog(message: String? = null): String {
  val stringWriter = StringWriter(256)
  if (message != null) {
    stringWriter.appendLine(message)
  }
  val printWriter = PrintWriter(stringWriter, false)
  printStackTrace(printWriter)
  printWriter.flush()
  return stringWriter.toString()
}
