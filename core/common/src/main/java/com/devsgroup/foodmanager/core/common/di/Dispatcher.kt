package com.devsgroup.foodmanager.core.common.di

import javax.inject.Qualifier

/** Qualifies an injected [kotlinx.coroutines.CoroutineDispatcher]. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: FoodDispatcher)

enum class FoodDispatcher {
    Default,
    IO,
}
