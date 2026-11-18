package com.ghostteam.findetect.data.mapper

import com.ghostteam.findetect.data.local.entity.CategoryEntity
import com.ghostteam.findetect.data.remote.dto.CategoryDto
import com.ghostteam.findetect.domain.model.Category
import com.ghostteam.findetect.domain.model.CategoryNames

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    key = key,
    names = CategoryNames(ru = nameRu, en = nameEn),
    iconId = iconId,
    isIncome = isIncome,
    isArchived = isArchived,
)

fun Category.toEntity(legacyEmoji: String? = null): CategoryEntity = CategoryEntity(
    id = id,
    key = key,
    nameRu = names.ru,
    nameEn = names.en,
    iconId = iconId,
    isIncome = isIncome,
    isArchived = isArchived,
    legacyEmoji = legacyEmoji,
)

fun CategoryDto.toDomain(): Category = Category(
    id = id,
    key = key,
    names = CategoryNames(ru = names.ru, en = names.en),
    iconId = iconId,
    isIncome = isIncome,
    isArchived = isArchived,
)

fun CategoryDto.toEntity(legacyEmoji: String? = null): CategoryEntity = toDomain().toEntity(legacyEmoji)
