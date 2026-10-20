package com.safespend.app.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs

/** 1999 -> "$19.99" (USD, always). */
fun Long.toMoney(): String {
    val sign = if (this < 0) "-" else ""
    val a = abs(this)
    return String.format(Locale.US, "%s\$%,d.%02d", sign, a / 100, a % 100)
}

/** "19.99" or "19,99" -> 1999. Returns null if not a valid number. */
fun String.toCents(): Long? = try {
    BigDecimal(trim().replace(',', '.'))
        .setScale(2, RoundingMode.HALF_UP)
        .movePointRight(2)
        .longValueExact()
} catch (e: Exception) {
    null
}

fun Long.toInputString(): String = String.format(Locale.US, "%d.%02d", this / 100, this % 100)
