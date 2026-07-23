package com.devsgroup.foodmanager.core.model

/** A recipe the user has created themselves (as opposed to a recommended [Food]). */
data class Recipe(
    val id: String,
    val name: String,
    val meta: String,
    val imageUrl: String? = null,
)
