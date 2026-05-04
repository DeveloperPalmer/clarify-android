package ru.sla.clarify.database.extension

import app.cash.sqldelight.Query

fun Query<Long>.executeAsStringOrZero() = this.executeAsOneOrNull()?.toString() ?: "0"
