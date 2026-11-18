package com.ghostteam.findetect.data.mapper

import com.ghostteam.findetect.data.local.model.AccountBalanceRow
import com.ghostteam.findetect.domain.model.AccountBalance
import com.ghostteam.findetect.domain.model.toMoneyAmount

fun AccountBalanceRow.toDomain(): AccountBalance = AccountBalance(
    account = account.toDomain(),
    income = incomeMinor.toMoneyAmount(),
    expense = expenseMinor.toMoneyAmount(),
    adjustments = adjustmentMinor.toMoneyAmount(),
)
