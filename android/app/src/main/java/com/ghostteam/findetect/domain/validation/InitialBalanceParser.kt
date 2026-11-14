package com.ghostteam.findetect.domain.validation

import java.math.BigDecimal
import javax.inject.Inject

/** Parses user input without rounding; unlike an operation, a new balance can be zero. */
class InitialBalanceParser @Inject constructor() {
    fun parse(text: String): BigDecimal? {
        val input = text.trim()
        if (!pattern.matches(input)) return null
        return BigDecimal(input.replace(',', '.')).setScale(2)
    }

    private companion object {
        val pattern = Regex("""[0-9]{1,9}(?:[.,][0-9]{1,2})?""")
    }
}
