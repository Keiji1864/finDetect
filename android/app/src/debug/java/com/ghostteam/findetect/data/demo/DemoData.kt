package com.ghostteam.findetect.data.demo

import com.ghostteam.findetect.data.remote.dto.AccountDto
import com.ghostteam.findetect.data.remote.dto.BalanceAdjustmentDto
import com.ghostteam.findetect.data.remote.dto.CategoryDto
import com.ghostteam.findetect.data.remote.dto.TransactionDto
import kotlinx.serialization.Serializable

/** Static debug fixtures shared by mock GET responses and the initial local database fill. */
@Serializable
data class DemoData(
    val categories: List<CategoryDto>,
    val accounts: List<AccountDto>,
    val transactions: List<TransactionDto>,
    val adjustments: List<BalanceAdjustmentDto>,
)
