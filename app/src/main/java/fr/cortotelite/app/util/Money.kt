package fr.cortotelite.app.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.round

private val euro = NumberFormat.getCurrencyInstance(Locale.FRANCE)

fun Double.euro(): String = euro.format(this)

fun Double.round2(): Double = round(this * 100.0) / 100.0

data class VatBreakdown(
    val ht: Double,
    val vat: Double,
    val ttc: Double,
    val rate: Double
) {
    fun display(preferTtc: Boolean): String =
        if (preferTtc) "${ttc.euro()} TTC" else "${ht.euro()} HT"
}

fun breakdown(ht: Double, vatRate: Double): VatBreakdown {
    val vat = (ht * vatRate / 100.0).round2()
    return VatBreakdown(ht.round2(), vat, (ht + vat).round2(), vatRate)
}

fun htFromTtc(ttc: Double, vatRate: Double): Double =
    if (vatRate <= 0) ttc.round2() else (ttc / (1 + vatRate / 100.0)).round2()
