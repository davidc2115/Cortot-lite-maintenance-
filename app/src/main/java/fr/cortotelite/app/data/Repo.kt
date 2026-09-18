package fr.cortotelite.app.data

import fr.cortotelite.app.util.breakdown
import fr.cortotelite.app.util.round2
import java.util.Calendar

class Repo(private val dao: AppDao) {
    fun company() = dao.company()
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

    suspend fun createDocument(clientId: Long, kind: DocumentKind, vatRate: Double, title: String): Long {
        val company = dao.companyNow() ?: CompanySettings()
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
        return dao.insertDocument(
            Document(clientId = clientId, kind = kind, number = number, vatRate = vatRate, title = title)
        )
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

    fun documentTotals(lines: List<DocumentLine>, vatRate: Double) =
        breakdown(lines.sumOf { it.quantity * it.unitPriceHt }, vatRate)
}
