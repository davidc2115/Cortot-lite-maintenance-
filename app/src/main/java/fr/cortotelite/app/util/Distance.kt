package fr.cortotelite.app.util

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Estimation distance route (km) entre deux adresses.
 * Utilise le Geocoder Android (OpenStreetMap / services système)
 * puis un facteur route ~1.35 sur la distance à vol d'oiseau.
 */
object Distance {
    /** Facteur vol d'oiseau → route (moyenne France métropolitaine). */
    private const val ROAD_FACTOR = 1.35

    data class Result(
        val oneWayKm: Double,
        val roundTripKm: Double,
        val fromResolved: String,
        val toResolved: String
    )

    suspend @Suppress("DEPRECATION")
    suspend fun estimate(
        context: Context,
        fromAddress: String,
        toAddress: String
    ): Result? = withContext(Dispatchers.IO) {
        if (fromAddress.isBlank() || toAddress.isBlank()) return@withContext null
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale.FRANCE)
        val from = geocoder.getFromLocationName(fromAddress, 1)?.firstOrNull() ?: return@withContext null
        val to = geocoder.getFromLocationName(toAddress, 1)?.firstOrNull() ?: return@withContext null
        val straight = haversineKm(from.latitude, from.longitude, to.latitude, to.longitude)
        val oneWay = (straight * ROAD_FACTOR).round1()
        Result(
            oneWayKm = oneWay,
            roundTripKm = (oneWay * 2).round1(),
            fromResolved = listOfNotNull(from.featureName, from.locality, from.postalCode).joinToString(", "),
            toResolved = listOfNotNull(to.featureName, to.locality, to.postalCode).joinToString(", ")
        )
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun Double.round1(): Double = round(this * 10.0) / 10.0
}
