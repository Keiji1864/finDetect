package com.ghostteam.findetect.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val key: String,
    val nameRu: String,
    val nameEn: String,
    val iconId: String,
    val isIncome: Boolean,
    val isArchived: Boolean = false,
    val legacyEmoji: String? = null,
)
