package com.ghostteam.findetect.domain.model

/** Stable keys shared with the API; unknown keys remain strings in stored models. */
enum class FinanceIcon(val key: String) {
    WALLET("wallet"), CHART("chart"), USER("user"), GEAR("gear"),
    BASKET("basket"), COFFEE("coffee"), BUS("bus"), CASH("cash"),
    CALENDAR("calendar"), TROPHY("trophy"), FLAME("flame"), SPARK("spark"),
    LOCK("lock"), RECEIPT("receipt"), PLUS("plus"), CHECK("check"),
}
