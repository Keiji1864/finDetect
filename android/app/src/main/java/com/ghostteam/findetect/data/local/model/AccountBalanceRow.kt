package com.ghostteam.findetect.data.local.model

import androidx.room.Embedded
import com.ghostteam.findetect.data.local.entity.AccountEntity

/** Read-only query projection, not an additional Room table. */
data class AccountBalanceRow(
    @Embedded val account: AccountEntity,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val adjustmentMinor: Long,
)
