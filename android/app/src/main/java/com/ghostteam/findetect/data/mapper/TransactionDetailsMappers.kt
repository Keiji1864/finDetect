package com.ghostteam.findetect.data.mapper

import com.ghostteam.findetect.data.local.model.TransactionDetailsRow
import com.ghostteam.findetect.domain.model.Category
import com.ghostteam.findetect.domain.model.CategoryNames
import com.ghostteam.findetect.domain.model.TransactionDetails

fun TransactionDetailsRow.toDomain(): TransactionDetails = TransactionDetails(
    transaction = transaction.toDomain(),
    currency = currency,
    category = Category(
        id = transaction.categoryId,
        key = categoryKey,
        names = CategoryNames(categoryNameRu, categoryNameEn),
        iconId = categoryIconId,
        isIncome = categoryIsIncome,
        isArchived = categoryIsArchived,
    ),
)
