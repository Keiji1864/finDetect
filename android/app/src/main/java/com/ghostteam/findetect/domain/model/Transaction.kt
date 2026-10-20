package com.ghostteam.findetect.domain.model

import kotlinx.datetime.Instant

data class Transaction(
    val id: String,           // UUID
    val accountId: String,
    val categoryId: String,
    val amount: Double,       // всегда > 0, доход/расход определяет статья
    val occurredAt: Instant,  // когда была операция
    val enteredAt: Instant,   // когда внесена (для серии)
    val title: String?,       // если пусто, в списке показываем название статьи
)
