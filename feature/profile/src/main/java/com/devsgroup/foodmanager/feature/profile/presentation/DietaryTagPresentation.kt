package com.devsgroup.foodmanager.feature.profile.presentation

import com.devsgroup.foodmanager.core.model.DietaryTag

/** Display copy for [DietaryTag] — the diet chips/sheet are the only place this is shown. */
internal val DietaryTag.emoji: String
    get() = when (this) {
        DietaryTag.VEGETARIAN -> "🥕"
        DietaryTag.VEGAN -> "🥦"
        DietaryTag.HALAL -> "☪️"
        DietaryTag.LACTOSE_FREE -> "🥛"
        DietaryTag.GLUTEN_FREE -> "🌾"
    }

internal val DietaryTag.label: String
    get() = when (this) {
        DietaryTag.VEGETARIAN -> "Вегетарианец"
        DietaryTag.VEGAN -> "Веган"
        DietaryTag.HALAL -> "Халяль"
        DietaryTag.LACTOSE_FREE -> "Без лактозы"
        DietaryTag.GLUTEN_FREE -> "Без глютена"
    }
