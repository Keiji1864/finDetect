package com.ghostteam.findetect.domain.model

data class Category(
    val id: String,
    val key: String,
    val names: CategoryNames,
    val iconId: String,
    val isIncome: Boolean,
    val isArchived: Boolean = false,
)

data class CategoryNames(val ru: String, val en: String)
