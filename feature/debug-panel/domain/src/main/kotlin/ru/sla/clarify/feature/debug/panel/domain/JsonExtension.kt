package ru.sla.clarify.feature.debug.panel.domain

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import ru.sla.clarify.feature.debug.panel.domain.entity.UserJsonError
import java.net.URI

internal fun parse(raw: String): JsonObject? {
  return runCatching { Json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
}

internal fun JsonObject.stringValue(field: String): String {
  val jsonElement = this[field] ?: error("Field $field is missing")
  return (jsonElement as JsonPrimitive).content
}

internal fun isHttpUrl(value: String): Boolean {
  val uri = runCatching { URI(value) }.getOrNull()
  if (uri == null) return false
  return uri.scheme?.lowercase() in HTTP_SCHEMES && !uri.host.isNullOrBlank()
}

internal fun Raise<UserJsonError>.requireString(obj: JsonObject, field: String): String {
  ensure(obj.containsKey(field)) { UserJsonError.MissingField(field) }
  return obj.stringValue(field)
}

internal fun Raise<UserJsonError>.requireHttpUrl(obj: JsonObject, field: String): String {
  ensure(obj.containsKey(field)) { UserJsonError.MissingField(field) }
  val url = obj.stringValue(field)
  ensure(isHttpUrl(url)) { UserJsonError.InvalidPhotoUrl }
  return url
}

internal fun Raise<UserJsonError>.ensureJsonSignature(obj: JsonObject, fields: Set<String>) {
  for (field in obj.keys) {
    val valid = fields.contains(field)
    if (!valid) raise(UserJsonError.UnknownField(field))
  }
}

private val HTTP_SCHEMES = setOf("http", "https")
