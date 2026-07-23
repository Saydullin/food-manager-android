package com.devsgroup.foodmanager.feature.profile.presentation

import com.devsgroup.foodmanager.core.model.DietaryTag

/** Display copy for [DietaryTag] — the diet chips/sheet are the only place this is shown. */
internal val DietaryTag.emoji: String
    get() = when (this) {
        DietaryTag.VEGETARIAN -> "🥕"
        DietaryTag.VEGAN -> "🥦"
        DietaryTag.GLUTEN_FREE -> "🌾"
        DietaryTag.DAIRY_FREE -> "🥛"
        DietaryTag.HALAL -> "☪️"
        DietaryTag.KOSHER -> "✡️"
        DietaryTag.LOW_CARB -> "🍞"
        DietaryTag.SPICY -> "🌶️"
    }

internal val DietaryTag.label: String
    get() = when (this) {
        DietaryTag.VEGETARIAN -> "Вегетарианец"
        DietaryTag.VEGAN -> "Веган"
        DietaryTag.GLUTEN_FREE -> "Без глютена"
        DietaryTag.DAIRY_FREE -> "Без лактозы"
        DietaryTag.HALAL -> "Халяль"
        DietaryTag.KOSHER -> "Кошер"
        DietaryTag.LOW_CARB -> "Низкоуглеводное"
        DietaryTag.SPICY -> "Люблю острое"
    }
