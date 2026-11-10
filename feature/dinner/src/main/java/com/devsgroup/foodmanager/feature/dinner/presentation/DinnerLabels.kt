package com.devsgroup.foodmanager.feature.dinner.presentation

import com.devsgroup.foodmanager.core.model.KitchenAppliance
import com.devsgroup.foodmanager.core.model.MedicalRestriction
import com.devsgroup.foodmanager.core.model.Sex

/**
 * Display copy for the dinner flow's vocabulary. Kept in the feature while nothing else
 * shows it; moves to `:core:ui` (next to the taste labels) once the profile screen
 * starts editing the household too.
 */

internal val KitchenAppliance.label: String
    get() = when (this) {
        KitchenAppliance.STOVE -> "Плита"
        KitchenAppliance.OVEN -> "Духовка"
        KitchenAppliance.AIR_FRYER -> "Аэрогриль"
        KitchenAppliance.MICROWAVE -> "Микроволновка"
        KitchenAppliance.GRILL -> "Гриль"
        KitchenAppliance.MULTICOOKER -> "Мультиварка"
        KitchenAppliance.BLENDER -> "Блендер"
    }

internal val KitchenAppliance.emoji: String
    get() = when (this) {
        KitchenAppliance.STOVE -> "🔥"
        KitchenAppliance.OVEN -> "♨️"
        KitchenAppliance.AIR_FRYER -> "🌀"
        KitchenAppliance.MICROWAVE -> "⚡"
        KitchenAppliance.GRILL -> "🍖"
        KitchenAppliance.MULTICOOKER -> "🍲"
        KitchenAppliance.BLENDER -> "🥤"
    }

internal val MedicalRestriction.label: String
    get() = when (this) {
        MedicalRestriction.LOW_CALORIE -> "Меньше калорий"
        MedicalRestriction.LOW_FAT -> "Меньше жира"
        MedicalRestriction.LOW_CARB -> "Меньше углеводов"
        MedicalRestriction.NO_SPICY -> "Без острого"
        MedicalRestriction.LOW_SALT -> "Меньше соли"
        MedicalRestriction.LOW_SUGAR -> "Меньше сахара"
    }

internal val Sex.label: String
    get() = when (this) {
        Sex.MALE -> "Мужской"
        Sex.FEMALE -> "Женский"
    }

/** The one non-cuisine "won't eat" option offered for now — the only feature tag we know dishes carry. */
internal const val SPICY_CODE = "SPICY"
