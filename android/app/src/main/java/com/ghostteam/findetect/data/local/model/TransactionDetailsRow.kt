package com.ghostteam.findetect.data.local.model

import androidx.room.Embedded
import com.ghostteam.findetect.data.local.entity.TransactionEntity

/** One reactive JOIN supplies historical category metadata without per-row lookups. */
data class TransactionDetailsRow(
    @Embedded val transaction: TransactionEntity,
    val currency: String,
    val categoryKey: String,
    val categoryNameRu: String,
    val categoryNameEn: String,
    val categoryIconId: String,
    val categoryIsIncome: Boolean,
    val categoryIsArchived: Boolean,
)
