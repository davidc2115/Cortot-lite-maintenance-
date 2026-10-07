package fr.cortotelite.app.data

import fr.cortotelite.app.ai.GeminiClient

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

    suspend fun createDocument(
        clientId: Long,
        kind: DocumentKind,
        vatRate: Double? = null,
        title: String = "Entretien PV et contrôle",
        autoLines: Boolean = true
    ): Long {
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
        if (autoLines) {
            val install = dao.installationFor(clientId)
            val kwc = install?.powerKwc ?: 0.0
            val unitKwc = if (company.pricePerKwcHt > 0) company.pricePerKwcHt else 40.0
            val forfaitMaint = if (company.maintenanceForfaitHt > 0) company.maintenanceForfaitHt else 80.0
            when (company.maintenanceBillingMode) {
                MaintenanceBillingMode.PUISSANCE -> {
                    if (kwc > 0) {
                        dao.insertLine(
                            DocumentLine(
                                documentId = docId,
                                label = "Entretien PV et contrôle — ${kwc} kWc × ${unitKwc.round2()} € HT/kWc",
                                quantity = kwc,
                                unitPriceHt = unitKwc,
                                sortOrder = 0
                            )
                        )
                    } else {
                        dao.insertLine(
                            DocumentLine(
                                documentId = docId,
                                label = "Entretien PV et contrôle (puissance non renseignée)",
                                quantity = 1.0,
                                unitPriceHt = unitKwc,
                                sortOrder = 0
                            )
                        )
                    }
                }
                MaintenanceBillingMode.FORFAIT -> {
                    dao.insertLine(
                        DocumentLine(
                            documentId = docId,
                            label = "Entretien PV et contrôle — forfait",
                            quantity = 1.0,
                            unitPriceHt = forfaitMaint,
                            sortOrder = 0
                        )
                    )
                }
            }
            // Déplacement auto selon réglage A/R ou aller simple
            val oneWay = client?.distanceKm ?: 0.0
            if (oneWay > 0 && company.travelRatePerKmHt > 0) {
                val isRt = company.travelRoundTripDefault
                val km = if (isRt) oneWay * 2 else oneWay
                val traj = if (isRt) "A/R" else "Aller simple"
                val ht = (km * company.travelRatePerKmHt).round2()
                val site = client?.siteAddress?.ifBlank { client.billingAddress }.orEmpty()
                val label = buildString {
                    append("Déplacement $traj — ${km.round2()} km")
                    append(" × ${company.travelRatePerKmHt.round2()} € HT/km")
                    if (company.address.isNotBlank() && site.isNotBlank()) {
                        append("\n${company.address.trim()} → ${site.trim()}")
                    }
                }
                dao.insertLine(
                    DocumentLine(documentId = docId, label = label, quantity = 1.0, unitPriceHt = ht, sortOrder = 1)
                )
                dao.insertTravel(
                    Travel(
                        clientId = clientId,
                        documentId = docId,
                        mode = TravelMode.KM,
                        kilometers = km,
                        amountHt = ht,
                        vatRate = rate,
                        comment = label.replace("\n", " | ")
                    )
                )
            }
        }
        return docId
    }

    suspend fun convertQuoteToInvoice(quoteId: Long): Long? {
        val quote = dao.document(quoteId) ?: return null
        if (quote.kind != DocumentKind.DEVIS) return null
        val newId = createDocument(quote.clientId, DocumentKind.FACTURE, quote.vatRate, quote.title, autoLines = false)
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


    // ── Employés ─────────────────────────────────────────────
    fun employees() = dao.employees()
    fun activeEmployees() = dao.activeEmployees()
    suspend fun employee(id: Long) = dao.employee(id)
    fun employeeWithPayslips(id: Long) = dao.employeeWithPayslips(id)

    suspend fun saveEmployee(employee: Employee): Long {
        return if (employee.id == 0L) dao.insertEmployee(employee)
        else { dao.updateEmployee(employee); employee.id }
    }

    suspend fun deleteEmployee(employee: Employee) = dao.deleteEmployee(employee)

    // ── Paie ─────────────────────────────────────────────────
    fun payslips() = dao.payslips()
    fun payslipsFor(employeeId: Long) = dao.payslipsFor(employeeId)
    suspend fun payslip(id: Long) = dao.payslip(id)

    /**
     * Génère ou met à jour la fiche de paie d'un employé pour un mois donné.
     * Commission basée uniquement sur les factures PAYE du mois.
     * CA EN_ATTENTE et RETARD sont affichés pour information.
     */
    suspend fun generatePayslip(
        employeeId: Long,
        year: Int,
        month: Int,
        primesBrut: Double = 0.0
    ): Payslip? {
        val emp = dao.employee(employeeId) ?: return null
        val company = dao.companyNow() ?: CompanySettings()

        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val fromMs = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val toMs = cal.timeInMillis

        val invoices = dao.invoicesInPeriod(fromMs, toMs)
        var caPaye = 0.0
        var caAttente = 0.0
        var caRetard = 0.0

        for (inv in invoices) {
            val lines = dao.lines(inv.id)
            val ht = lines.sumOf { it.quantity * it.unitPriceHt }
            when (inv.status) {
                DocumentStatus.PAYE -> caPaye += ht
                DocumentStatus.EN_ATTENTE -> caAttente += ht
                DocumentStatus.RETARD -> caRetard += ht
                DocumentStatus.ENVOYE -> caAttente += ht
                else -> {}
            }
        }

        val commissionPct = if (emp.commissionPercent > 0) emp.commissionPercent
                            else company.commissionPercent
        val commission = (caPaye * commissionPct / 100.0).round2()
        val base = emp.baseSalaryBrut
        val brutTotal = (base + commission + primesBrut).round2()
        // Estimation simplifiée charges salariales ~22 %
        val charges = (brutTotal * 0.22).round2()
        val net = (brutTotal - charges).round2()

        val existing = dao.payslipForPeriod(employeeId, year, month)
        val slip = Payslip(
            id = existing?.id ?: 0,
            employeeId = employeeId,
            year = year,
            month = month,
            status = existing?.status ?: PayslipStatus.BROUILLON,
            baseBrut = base,
            commissionBrut = commission,
            primesBrut = primesBrut,
            chargesSalariales = charges,
            netAPayer = net,
            caPayeHt = caPaye.round2(),
            caEnAttenteHt = caAttente.round2(),
            caRetardHt = caRetard.round2(),
            notes = existing?.notes.orEmpty(),
            generatedAt = System.currentTimeMillis(),
            paidAt = existing?.paidAt
        )
        if (slip.id == 0L) {
            val id = dao.insertPayslip(slip)
            return slip.copy(id = id)
        } else {
            dao.updatePayslip(slip)
            return slip
        }
    }

    /** Génère les fiches de tous les employés actifs pour le mois. */
    suspend fun generateAllPayslips(year: Int, month: Int): Int {
        // Note: activeEmployees() is Flow; we generate for each employee fetched via a helper.
        // Pour simplicité, l'UI appelle generatePayslip par employé.
        return 0
    }

    suspend fun updatePayslipStatus(id: Long, status: PayslipStatus) {
        val p = dao.payslip(id) ?: return
        val paidAt = if (status == PayslipStatus.PAYE) System.currentTimeMillis() else p.paidAt
        dao.updatePayslip(p.copy(status = status, paidAt = paidAt))
    }

    suspend fun deletePayslip(payslip: Payslip) = dao.deletePayslip(payslip)

    // ── Dividendes ───────────────────────────────────────────
    fun dividends() = dao.dividends()
    fun dividendsFor(employeeId: Long) = dao.dividendsFor(employeeId)

    /**
     * Calcule et enregistre un dividende pour un dirigeant sur une année.
     * Base = somme HT des factures PAYE de l'année × dividendPercent de l'employé.
     */
    suspend fun calculateDividend(employeeId: Long, year: Int, notes: String = ""): Dividend? {
        val emp = dao.employee(employeeId) ?: return null
        if (emp.dividendPercent <= 0) return null

        val cal = Calendar.getInstance()
        cal.set(year, 0, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val fromMs = cal.timeInMillis
        cal.set(year + 1, 0, 1, 0, 0, 0)
        val toMs = cal.timeInMillis

        val invoices = dao.invoicesInPeriod(fromMs, toMs)
            .filter { it.status == DocumentStatus.PAYE }
        var ca = 0.0
        for (inv in invoices) {
            ca += dao.lines(inv.id).sumOf { it.quantity * it.unitPriceHt }
        }
        val amount = (ca * emp.dividendPercent / 100.0).round2()
        val div = Dividend(
            employeeId = employeeId,
            year = year,
            amount = amount,
            percent = emp.dividendPercent,
            baseCaHt = ca.round2(),
            notes = notes
        )
        val id = dao.insertDividend(div)
        return div.copy(id = id)
    }

    suspend fun updateDividend(d: Dividend) = dao.updateDividend(d)
    suspend fun deleteDividend(d: Dividend) = dao.deleteDividend(d)

    /**
     * Met à jour automatiquement le statut des factures :
     * ENVOYE/EN_ATTENTE dont dueAt dépassé → RETARD.
     */
    suspend fun refreshInvoiceStatuses() {
        val company = dao.companyNow() ?: CompanySettings()
        val delayMs = company.paymentDelayDays * 24L * 3600_000L
        val now = System.currentTimeMillis()
        // On parcourt via invoicesInPeriod large range — simple approach:
        // Utilise documents() flow would need collection; for now update known ones
        // via a broad period (last 3 years)
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -3)
        val from = cal.timeInMillis
        val invoices = dao.invoicesInPeriod(from, now + 1)
        for (inv in invoices) {
            if (inv.status == DocumentStatus.PAYE || inv.status == DocumentStatus.REFUSE) continue
            val due = inv.dueAt ?: (inv.issuedAt + delayMs)
            if (now > due && inv.status != DocumentStatus.RETARD) {
                dao.updateDocument(inv.copy(status = DocumentStatus.RETARD))
            } else if (inv.status == DocumentStatus.ENVOYE && inv.kind == DocumentKind.FACTURE) {
                // passe en EN_ATTENTE si pas encore fait
                dao.updateDocument(inv.copy(status = DocumentStatus.EN_ATTENTE, dueAt = due))
            }
        }
    }

    /** Marque une facture comme payée. */
    suspend fun markInvoicePaid(documentId: Long) {
        val doc = dao.document(documentId) ?: return
        if (doc.kind != DocumentKind.FACTURE) return
        dao.updateDocument(
            doc.copy(
                status = DocumentStatus.PAYE,
                paidAt = System.currentTimeMillis()
            )
        )
    }

    fun geminiClient(): GeminiClient? {
        // Called from UI with company settings
        return null
    }
}
