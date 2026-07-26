package com.devsgroup.foodmanager.core.model

import java.util.Locale

/** The app's display language. [tag] is the BCP-47 tag the food API's `?lang=` query param expects. */
enum class AppLanguage(val tag: String) {
    RUSSIAN("ru"),
    ENGLISH("en"),
    ;

    companion object {
        /** Resolves the closest supported [AppLanguage] for a BCP-47 [tag], defaulting to [ENGLISH]. */
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: ENGLISH

        /** The device's current locale, mapped to the closest supported language. */
        fun systemDefault(): AppLanguage = fromTag(Locale.getDefault().language)
    }
}
