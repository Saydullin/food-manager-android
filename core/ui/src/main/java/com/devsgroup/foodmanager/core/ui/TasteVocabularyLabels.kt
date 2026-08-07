package com.devsgroup.foodmanager.core.ui

import com.devsgroup.foodmanager.core.model.Allergen
import com.devsgroup.foodmanager.core.model.Cuisine
import com.devsgroup.foodmanager.core.model.DietaryTag

/**
 * Display copy for the taste vocabulary the user picks from during onboarding.
 *
 * The split this file exists to serve: the taste lists are stored and sent as the
 * server's canonical codes (`ITALIAN`, `TREE_NUTS`) because that's what the feed's
 * ranking and filtering match on exactly — but those same stored strings are rendered
 * as chips on the profile screen, where a raw `TREE_NUTS` would be user-visible. So
 * codes travel on the wire and through DataStore, and get translated here at the point
 * of display.
 *
 * Lives in `:core:ui` rather than in a feature because both `:feature:onboarding` (which
 * writes these) and `:feature:profile` (which shows and edits them) need the same
 * mapping, and features never depend on each other.
 *
 * Strings are hardcoded Russian, matching how the rest of this app's UI copy is written
 * (see the profile feature's `DietaryTagPresentation`). Worth moving all of it to string
 * resources together if the app is ever localized — doing it for this one file alone
 * would just make the codebase inconsistent.
 */

val Cuisine.label: String
    get() = when (this) {
        Cuisine.ITALIAN -> "Итальянская"
        Cuisine.JAPANESE -> "Японская"
        Cuisine.CHINESE -> "Китайская"
        Cuisine.MEXICAN -> "Мексиканская"
        Cuisine.INDIAN -> "Индийская"
        Cuisine.THAI -> "Тайская"
        Cuisine.FRENCH -> "Французская"
        Cuisine.AMERICAN -> "Американская"
        Cuisine.MEDITERRANEAN -> "Средиземноморская"
        Cuisine.KOREAN -> "Корейская"
    }

val Cuisine.emoji: String
    get() = when (this) {
        Cuisine.ITALIAN -> "🍝"
        Cuisine.JAPANESE -> "🍣"
        Cuisine.CHINESE -> "🥡"
        Cuisine.MEXICAN -> "🌮"
        Cuisine.INDIAN -> "🍛"
        Cuisine.THAI -> "🍲"
        Cuisine.FRENCH -> "🥐"
        Cuisine.AMERICAN -> "🍔"
        Cuisine.MEDITERRANEAN -> "🫒"
        Cuisine.KOREAN -> "🍜"
    }

val Allergen.label: String
    get() = when (this) {
        Allergen.MILK -> "Молоко"
        Allergen.EGGS -> "Яйца"
        Allergen.PEANUTS -> "Арахис"
        Allergen.TREE_NUTS -> "Орехи"
        Allergen.SOY -> "Соя"
        Allergen.WHEAT -> "Пшеница"
        Allergen.GLUTEN -> "Глютен"
        Allergen.FISH -> "Рыба"
        Allergen.SHELLFISH -> "Ракообразные"
        Allergen.SESAME -> "Кунжут"
        Allergen.MUSTARD -> "Горчица"
        Allergen.CELERY -> "Сельдерей"
        Allergen.LUPIN -> "Люпин"
        Allergen.MOLLUSCS -> "Моллюски"
        Allergen.SULFITES -> "Сульфиты"
    }

val DietaryTag.label: String
    get() = when (this) {
        DietaryTag.VEGETARIAN -> "Вегетарианец"
        DietaryTag.VEGAN -> "Веган"
        DietaryTag.HALAL -> "Халяль"
        DietaryTag.LACTOSE_FREE -> "Без лактозы"
        DietaryTag.GLUTEN_FREE -> "Без глютена"
    }

val DietaryTag.emoji: String
    get() = when (this) {
        DietaryTag.VEGETARIAN -> "🥕"
        DietaryTag.VEGAN -> "🥦"
        DietaryTag.HALAL -> "☪️"
        DietaryTag.LACTOSE_FREE -> "🥛"
        DietaryTag.GLUTEN_FREE -> "🌾"
    }

/** Every code this file can translate, resolved once rather than per chip. */
private val labelsByCode: Map<String, String> =
    Cuisine.entries.associate { it.name to it.label } + Allergen.entries.associate { it.name to it.label }

/**
 * The display label for one stored taste tag.
 *
 * Free text the user typed themselves is passed through untouched — the taste lists
 * accept both (onboarding contributes codes, the profile screen's "Добавить" sheet
 * contributes whatever the user writes), and only the codes have a translation to look
 * up. So this is a lookup with a passthrough default, not a validation.
 */
fun tasteTagLabel(code: String): String = labelsByCode[code.trim().uppercase()] ?: code
