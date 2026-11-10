package com.devsgroup.foodmanager.core.model

/** Cooking equipment the user has at home. */
enum class KitchenAppliance {
    STOVE,
    OVEN,
    AIR_FRYER,
    MICROWAVE,
    GRILL,
    MULTICOOKER,
    BLENDER,
}

/** Where the ingredients for tonight's dinner come from. */
enum class IngredientSource {
    /** Only what's already at home — needs the pantry and structured ingredients (not built yet). */
    AT_HOME,

    /** Anything; missing ingredients get bought. */
    ANY,
}
