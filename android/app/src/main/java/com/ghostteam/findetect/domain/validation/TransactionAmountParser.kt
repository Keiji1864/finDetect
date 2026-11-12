package com.ghostteam.findetect.domain.validation

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/** Accepts decimal user input with a dot or comma. Fractional kopecks are rejected, never rounded. */
class TransactionAmountParser @Inject constructor() {
    fun parse(text: String): BigDecimal? {
        val input = text.trim()
        if (!amountPattern.matches(input)) return null
        val amount = BigDecimal(input.replace(',', '.'))
        return if (amount.signum() > 0) amount.setScale(2, RoundingMode.UNNECESSARY) else null
    }
}

private val amountPattern = Regex("""[0-9]{1,9}(?:[.,][0-9]{1,2})?""")
