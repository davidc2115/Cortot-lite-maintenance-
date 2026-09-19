package fr.cortotelite.app.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.round

private val euro = NumberFormat.getCurrencyInstance(Locale.FRANCE)

fun Double.euro(): String = euro.format(this)

fun Double.round2(): Double = round(this * 100.0) / 100.0

data class VatBreakdown(
    val htBrut: Double,
    val discountHt: Double,
    val ht: Double,
    val vatBrut: Double,
    val vatDiscount: Double,
    val vat: Double,
    val ttc: Double,
    val rate: Double,
    val discountPercent: Double,
    val vatDiscountPercent: Double
) {
    fun display(preferTtc: Boolean): String =
        if (preferTtc) "${ttc.euro()} TTC" else "${ht.euro()} HT"
}

fun breakdown(
    htBrut: Double,
    vatRate: Double,
    discountPercent: Double = 0.0,
    vatDiscountPercent: Double = 0.0
): VatBreakdown {
    val disc = (htBrut * discountPercent / 100.0).round2()
    val ht = (htBrut - disc).round2()
    val vatBrut = (ht * vatRate / 100.0).round2()
    val vatDisc = (vatBrut * vatDiscountPercent / 100.0).round2()
    val vat = (vatBrut - vatDisc).round2()
    return VatBreakdown(
        htBrut = htBrut.round2(),
        discountHt = disc,
        ht = ht,
        vatBrut = vatBrut,
        vatDiscount = vatDisc,
        vat = vat,
        ttc = (ht + vat).round2(),
        rate = vatRate,
        discountPercent = discountPercent,
        vatDiscountPercent = vatDiscountPercent
    )
}

fun htFromTtc(ttc: Double, vatRate: Double): Double =
    if (vatRate <= 0) ttc.round2() else (ttc / (1 + vatRate / 100.0)).round2()
