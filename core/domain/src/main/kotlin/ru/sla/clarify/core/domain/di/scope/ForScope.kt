package ru.sla.clarify.core.domain.di.scope

import javax.inject.Qualifier
import kotlin.reflect.KClass

/**
 * Отличает экземпляр, принадлежащий скоупу [value], от такого же типа из других скоупов — например
 * корутин-скоуп флоу от корутин-скоупа его родителя.
 *
 * Как и в [SingleIn], [value] служит только ключом.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ForScope(val value: KClass<*>)
