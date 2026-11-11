package com.ghostteam.findetect.domain.model

import kotlinx.datetime.Instant

data class TransactionFilter(
    val currency: String? = null,
    val accountId: String? = null,
    val categoryId: String? = null,
    val isIncome: Boolean? = null,
    val fromInclusive: Instant? = null,
    val toExclusive: Instant? = null,
) {
    init {
        require(fromInclusive == null || toExclusive == null || fromInclusive <= toExclusive) {
            "Period start must not follow its end"
        }
    }
}

data class AdjustmentFilter(
    val currency: String? = null,
    val accountId: String? = null,
    val fromInclusive: Instant? = null,
    val toExclusive: Instant? = null,
) {
    init {
        require(fromInclusive == null || toExclusive == null || fromInclusive <= toExclusive) {
            "Period start must not follow its end"
        }
    }
}
