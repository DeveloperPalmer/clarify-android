package ru.sla.clarify.core.domain.di.scope

import me.tatarka.inject.annotations.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ActivityContext

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationContext
