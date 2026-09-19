package fr.cortotelite.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import fr.cortotelite.app.data.Client
import fr.cortotelite.app.data.CompanySettings
import fr.cortotelite.app.data.Document
import fr.cortotelite.app.data.DocumentKind
import fr.cortotelite.app.data.DocumentLine
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExport {
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

    fun buildPdf(
        context: Context,
        company: CompanySettings,
        client: Client?,
        document: Document,
        lines: List<DocumentLine>,
        totals: VatBreakdown
    ): File {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 points
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas
        val title = Paint().apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val body = Paint().apply {
            textSize = 11f
            isAntiAlias = true
        }
        val small = Paint().apply {
            textSize = 9f
            isAntiAlias = true
        }
        var y = 40f
        fun line(text: String, paint: Paint = body) {
            canvas.drawText(text, 40f, y, paint)
            y += paint.textSize + 6f
        }

        val kind = if (document.kind == DocumentKind.DEVIS) "DEVIS" else "FACTURE"
        line(company.name.ifBlank { "Cortot Élite" }, title)
        if (company.legalName.isNotBlank()) line(company.legalName, small)
        if (company.address.isNotBlank()) line(company.address, small)
        if (company.siret.isNotBlank()) line("SIRET : ${company.siret}", small)
        if (company.tvaNumber.isNotBlank()) line("N° TVA : ${company.tvaNumber}", small)
        if (company.phone.isNotBlank()) line("Tél. : ${company.phone}", small)
        if (company.email.isNotBlank()) line("E-mail : ${company.email}", small)
        y += 12f
        line("$kind n° ${document.number}", title)
        line("Date : ${dateFmt.format(Date(document.issuedAt))}")
        line("Statut : ${document.status.name}")
        if (document.title.isNotBlank()) line("Objet : ${document.title}")
        y += 10f
        line("Client", title)
        line(clientDisplay(client))
        client?.billingAddress?.takeIf { it.isNotBlank() }?.let { line("Facturation : $it") }
        client?.siteAddress?.takeIf { it.isNotBlank() }?.let { line("Chantier : $it") }
        client?.phoneMobile?.takeIf { it.isNotBlank() }?.let { line("Tél. : $it") }
        client?.email?.takeIf { it.isNotBlank() }?.let { line("E-mail : $it") }
        y += 12f
        line("Détail des prestations", title)
        lines.forEachIndexed { i, l ->
            val ht = (l.quantity * l.unitPriceHt).round2()
            line("${i + 1}. ${l.label.take(70)}")
            line("    ${l.quantity} × ${l.unitPriceHt.euro()} HT  →  ${ht.euro()} HT", small)
            if (y > 760f) {
                // simple: stop if overflow (single page for v1)
                line("…", small)
                return@forEachIndexed
            }
        }
        y += 10f
        if (totals.discountPercent > 0) line("Remise commerciale ${totals.discountPercent} % : -${totals.discountHt.euro()}")
        line("Total HT : ${totals.ht.euro()}")
        if (totals.vatDiscountPercent > 0) line("Remise TVA ${totals.vatDiscountPercent} % : -${totals.vatDiscount.euro()}")
        line("TVA ${totals.rate.toInt()} % : ${totals.vat.euro()}")
        line("Total TTC : ${totals.ttc.euro()}", title)
        y += 16f
        line(
            "Mentions : pénalités de retard au taux légal. Pas d'escompte pour paiement anticipé.",
            small
        )
        if (company.siret.isNotBlank()) line("SIRET ${company.siret}", small)

        pdf.finishPage(page)
        val dir = File(context.cacheDir, "docs").apply { mkdirs() }
        val file = File(dir, "${document.number.replace('/', '-')}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return file
    }

    fun sharePdf(context: Context, file: File, subject: String, email: String? = null) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            if (!email.isNullOrBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_TEXT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Envoyer / enregistrer"))
    }

    fun dial(context: Context, phone: String) {
        val cleaned = phone.filter { it.isDigit() || it == '+' }
        if (cleaned.isBlank()) return
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")))
    }

    fun emailOnly(context: Context, email: String, subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(intent, "Envoyer par e-mail"))
    }

    private fun clientDisplay(c: Client?): String {
        if (c == null) return "—"
        return if (c.companyName.isNotBlank()) c.companyName
        else "${c.firstName} ${c.lastName}".trim().ifBlank { "Client" }
    }
}
