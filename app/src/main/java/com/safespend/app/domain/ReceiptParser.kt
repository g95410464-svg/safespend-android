package com.safespend.app.domain

import java.text.Normalizer

data class ParsedReceipt(
    val amountCents: Long?,
    /** true => ATM withdrawal slip: internal transfer Bank -> Cash, NOT an expense. */
    val isWithdrawal: Boolean,
    val merchant: String,
    val category: String,
    val rawText: String
)

/** Pure Kotlin, no Android deps: easy to unit test. */
object ReceiptParser {

    private val withdrawalRegex =
        Regex("""\b(RETIRO|DISPOSICION|WITHDRAWAL|CASH\s+ADVANCE|ATM|CAJERO)\b""")
    private val totalKeyRegex =
        Regex("""\b(TOTAL|IMPORTE|A\s+PAGAR|MONTO|AMOUNT|BALANCE\s+DUE|RETIRO|WITHDRAWAL)\b""")
    private val subtotalRegex = Regex("""\bSUB\s*TOTAL\b""")
    private val moneyRegex =
        Regex("""(?<![\d.,])(\d{1,3}(?:[.,]\d{3})*[.,]\d{2}|\d+[.,]\d{2})(?!\d)""")
    private val noiseRegex =
        Regex("""\b(FACTURA|TICKET|RECIBO|RECEIPT|FECHA|DATE|NIT|RUC|IVA|TEL|HORA|CAJA|FOLIO)\b""")

    private val categoryRules: List<Pair<Regex, String>> = listOf(
        Regex("""GASOLINERA|COMBUSTIBLE|DIESEL|GASOLINA""") to "Transporte",
        Regex("""UBER|TAXI|PARQUEO|PARKING""") to "Transporte",
        Regex("""FARMACIA|PHARMA|DROGUERIA|CLINICA""") to "Salud",
        Regex("""RESTAURANT|PIZZA|POLLO|CAFE|BURGER|COMEDOR|PUPUSERIA""") to "Comida",
        Regex("""SUPERMERCADO|SUPERMARKET|MARKET|MERCADO|SELECTOS|WALMART|DESPENSA""") to "Supermercado"
    )

    fun parse(rawText: String): ParsedReceipt {
        val norm = normalize(rawText)
        val rawLines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val lines = norm.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val withdrawal = withdrawalRegex.containsMatchIn(norm)
        val amount = findAmount(lines)

        val merchant = if (withdrawal) "Retiro en cajero" else findMerchant(rawLines)
        val category = if (withdrawal) "Retiro"
        else categoryRules.firstOrNull { it.first.containsMatchIn(norm) }?.second ?: "Otros"

        return ParsedReceipt(amount, withdrawal, merchant, category, rawText)
    }

    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").uppercase()

    private fun amountsIn(line: String): List<Long> =
        moneyRegex.findAll(line).mapNotNull { parseAmount(it.value) }.toList()

    /** "1,234.56" / "1.234,56" / "19.99" -> cents. Last separator is the decimal one. */
    private fun parseAmount(s: String): Long? {
        val sep = maxOf(s.lastIndexOf('.'), s.lastIndexOf(','))
        if (sep < 0) return null
        val whole = s.substring(0, sep).filter(Char::isDigit).toLongOrNull() ?: return null
        val frac = s.substring(sep + 1).toLongOrNull() ?: return null
        return whole * 100 + frac
    }

    private fun findAmount(lines: List<String>): Long? {
        val keyed = mutableListOf<Long>()
        lines.forEachIndexed { i, line ->
            if (totalKeyRegex.containsMatchIn(line) && !subtotalRegex.containsMatchIn(line)) {
                // OCR often puts the number on the next line
                val found = amountsIn(line).ifEmpty {
                    lines.getOrNull(i + 1)?.let(::amountsIn).orEmpty()
                }
                keyed += found
            }
        }
        return keyed.maxOrNull() ?: lines.flatMap(::amountsIn).maxOrNull()
    }

    private fun findMerchant(rawLines: List<String>): String =
        rawLines.take(6).firstOrNull { line ->
            val letters = line.count { it.isLetter() }
            letters >= 3 &&
                letters.toFloat() / line.length >= 0.6f &&
                !noiseRegex.containsMatchIn(normalize(line))
        }?.take(40) ?: ""
}

