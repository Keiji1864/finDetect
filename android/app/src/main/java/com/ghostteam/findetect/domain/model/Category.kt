package com.ghostteam.findetect.domain.model

data class Category(
    val id: String,
    val key: String,
    val names: CategoryNames,
    val iconId: String,
    val isIncome: Boolean,
    val isArchived: Boolean = false,
) {
    val isCustom: Boolean get() = key == customKey(id)

    companion object {
        const val MAX_CUSTOM_NAME_LENGTH = 256

        fun customKey(id: String): String = "custom_$id"
    }
}

data class CategoryNames(val ru: String, val en: String)

data class CategorySetting(val category: Category, val enabled: Boolean)
