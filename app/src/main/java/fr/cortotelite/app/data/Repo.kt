package fr.cortotelite.app.data

import android.content.Context
import fr.cortotelite.app.util.Distance
import fr.cortotelite.app.util.breakdown
import fr.cortotelite.app.util.round2
import java.util.Calendar

class Repo(private val dao: AppDao) {
    fun company() = dao.company()
    suspend fun companyNow() = dao.companyNow()
    fun clients() = dao.clients()
    fun clientWithInstall(id: Long) = dao.clientWithInstall(id)
    fun documents() = dao.documents()
    fun documentsFor(id: Long) = dao.documentsFor(id)
    fun documentWithLines(id: Long) = dao.documentWithLines(id)
    fun travels() = dao.travels()
    fun travelsFor(id: Long) = dao.travelsFor(id)

    suspend fun saveCompany(settings: CompanySettings) = dao.upsertCompany(settings)

    suspend fun saveClient(client: Client, install: Installation): Long {
        val id = if (client.id == 0L) dao.insertClient(client) else {
            dao.updateClient(client); client.id
        }
        val existing = dao.installationFor(id)
        val inst = install.copy(clientId = id, id = existing?.id ?: 0)
        if (inst.id == 0L) dao.insertInstallation(inst) else dao.updateInstallation(inst)
        return id
    }

    suspend fun deleteClient(client: Client) = dao.deleteClient(client)

    suspend fun createDocument(clientId: Long, kind: DocumentKind, vatRate: Double? = null, title: String = "Installation / maintenance PV"): Long {
        val company = dao.companyNow() ?: CompanySettings()
        val client = dao.client(clientId)
        // TVA auto : particulier → taux réduit, professionnel → taux normal
        val autoVat = when (client?.type) {
            ClientType.PARTICULIER -> company.reducedVatRate
            ClientType.PROFESSIONNEL -> company.defaultVatRate
            null -> company.defaultVatRate
        }
        val rate = vatRate ?: autoVat
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val (prefix, n) = if (kind == DocumentKind.DEVIS) {
            "D" to company.nextQuoteNumber
        } else {
            "F" to company.nextInvoiceNumber
        }
        val number = "$prefix-$year-${n.toString().padStart(4, '0')}"
        val updated = if (kind == DocumentKind.DEVIS)
            company.copy(nextQuoteNumber = n + 1)
        else company.copy(nextInvoiceNumber = n + 1)
        dao.upsertCompany(updated)
        val docId = dao.insertDocument(
            Document(clientId = clientId, kind = kind, number = number, vatRate = rate, title = title)
        )
        // Ligne auto selon puissance installation (kWc × tarif société)
        val install = dao.installationFor(clientId)
        val kwc = install?.powerKwc ?: 0.0
        if (kwc > 0 && company.pricePerKwcHt > 0) {
            dao.insertLine(
                DocumentLine(
                    documentId = docId,
                    label = "Installation photovoltaïque — ${kwc} kWc × ${company.pricePerKwcHt.round2()} € HT/kWc",
                    quantity = kwc,
                    unitPriceHt = company.pricePerKwcHt,
                    sortOrder = 0
                )
            )
        }
        return docId
    }

    suspend fun convertQuoteToInvoice(quoteId: Long): Long? {
        val quote = dao.document(quoteId) ?: return null
        if (quote.kind != DocumentKind.DEVIS) return null
        val newId = createDocument(quote.clientId, DocumentKind.FACTURE, quote.vatRate, quote.title)
        dao.lines(quoteId).forEach {
            dao.insertLine(it.copy(id = 0, documentId = newId))
        }
        dao.updateDocument(quote.copy(status = DocumentStatus.ACCEPTE))
        dao.updateDocument(dao.document(newId)!!.copy(convertedFromQuoteId = quoteId))
        return newId
    }

    suspend fun updateDocument(doc: Document) = dao.updateDocument(doc)
    suspend fun deleteDocument(doc: Document) = dao.deleteDocument(doc)
    suspend fun addLine(line: DocumentLine) = dao.insertLine(line)
    suspend fun updateLine(line: DocumentLine) = dao.updateLine(line)
    suspend fun deleteLine(line: DocumentLine) = dao.deleteLine(line)

    suspend fun saveTravel(travel: Travel, attachAsLine: Boolean) {
        val company = dao.companyNow() ?: CompanySettings()
        val ht = when (travel.mode) {
            TravelMode.FORFAIT -> company.travelForfaitHt
            TravelMode.KM -> (travel.kilometers * company.travelRatePerKmHt).round2()
            TravelMode.TEMPS -> (travel.hours * company.travelHourlyHt).round2()
        }
        val id = if (travel.id == 0L) dao.insertTravel(travel.copy(amountHt = ht))
        else { dao.updateTravel(travel.copy(amountHt = ht)); travel.id }
        if (attachAsLine && travel.documentId != null) {
            val label = when (travel.mode) {
                TravelMode.FORFAIT -> "Déplacement forfait"
                TravelMode.KM -> "Déplacement ${travel.kilometers} km"
                TravelMode.TEMPS -> "Déplacement ${travel.hours} h"
            }
            dao.insertLine(DocumentLine(documentId = travel.documentId, label = label, unitPriceHt = ht))
        }
        id
    }

    suspend fun deleteTravel(travel: Travel) = dao.deleteTravel(travel)

    /**
     * Estime la distance société → chantier client, met à jour le cache client.distanceKm (aller simple).
     * @return km aller simple, ou null si géocodage impossible.
     */
    suspend fun refreshClientDistance(context: Context, clientId: Long): Double? {
        val company = dao.companyNow() ?: return null
        val client = dao.client(clientId) ?: return null
        val site = client.siteAddress.ifBlank { client.billingAddress }
        if (company.address.isBlank() || site.isBlank()) return null
        val result = Distance.estimate(context, company.address, site) ?: return null
        dao.updateClient(client.copy(distanceKm = result.oneWayKm))
        return result.oneWayKm
    }

    /**
     * Ajoute une ligne de déplacement détaillée sur un document.
     * Utilise distanceKm en cache, sinon tente un calcul, sinon [fallbackKm].
     * @param roundTrip si null, utilise le réglage société.
     */
    suspend fun addAutoTravelLine(
        context: Context,
        documentId: Long,
        clientId: Long,
        roundTrip: Boolean? = null,
        fallbackKm: Double = 0.0
    ): DocumentLine? {
        val company = dao.companyNow() ?: CompanySettings()
        val client = dao.client(clientId) ?: return null
        var oneWay = client.distanceKm
        if (oneWay <= 0) {
            oneWay = refreshClientDistance(context, clientId) ?: fallbackKm
        }
        if (oneWay <= 0) return null
        val isRt = roundTrip ?: company.travelRoundTripDefault
        val km = if (isRt) (oneWay * 2) else oneWay
        val ht = (km * company.travelRatePerKmHt).round2()
        val site = client.siteAddress.ifBlank { client.billingAddress }
        val traj = if (isRt) "A/R" else "Aller simple"
        val label = buildString {
            append("Déplacement $traj — ${km.round2()} km")
            append(" × ${company.travelRatePerKmHt.round2()} € HT/km")
            if (company.address.isNotBlank() && site.isNotBlank()) {
                append("\n${company.address.trim()} → ${site.trim()}")
            }
        }
        // Remplace l'ancienne ligne déplacement au lieu d'empiler
        dao.lines(documentId)
            .filter { it.label.startsWith("Déplacement") }
            .forEach { dao.deleteLine(it) }
        dao.travelsForDocument(documentId)
            .filter { it.mode == TravelMode.KM }
            .forEach { dao.deleteTravel(it) }

        val line = DocumentLine(
            documentId = documentId,
            label = label,
            quantity = 1.0,
            unitPriceHt = ht
        )
        val id = dao.insertLine(line)
        dao.insertTravel(
            Travel(
                clientId = clientId,
                documentId = documentId,
                mode = TravelMode.KM,
                kilometers = km,
                amountHt = ht,
                vatRate = company.defaultVatRate,
                comment = label.replace("\n", " | ")
            )
        )
        return line.copy(id = id)
    }

    fun documentTotals(
        lines: List<DocumentLine>,
        vatRate: Double,
        discountPercent: Double = 0.0,
        vatDiscountPercent: Double = 0.0
    ) = breakdown(
        lines.sumOf { it.quantity * it.unitPriceHt },
        vatRate,
        discountPercent,
        vatDiscountPercent
    )
}
