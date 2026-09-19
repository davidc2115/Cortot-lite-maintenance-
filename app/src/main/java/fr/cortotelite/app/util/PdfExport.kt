package fr.cortotelite.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
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
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val c = page.canvas
        val black = Color.rgb(20, 20, 20)
        val gray = Color.rgb(90, 90, 90)
        val lineGray = Color.rgb(220, 220, 220)
        val headerBg = Color.rgb(25, 25, 25)
        val totalBg = Color.rgb(25, 25, 25)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = black
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = black
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = black
            textSize = 10f
        }
        val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gray
            textSize = 9f
        }
        val whiteBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val fillBlack = Paint().apply { color = headerBg; style = Paint.Style.FILL }
        val fillTotal = Paint().apply { color = totalBg; style = Paint.Style.FILL }
        val stroke = Paint().apply {
            color = lineGray
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val kind = if (document.kind == DocumentKind.DEVIS) "DEVIS" else "FACTURE"
        var y = 48f

        // Title
        c.drawText(kind, 40f, y, titlePaint)
        y += 22f

        // Badges number + date
        val badge1 = "${if (document.kind == DocumentKind.DEVIS) "Devis" else "Facture"} n°${document.number}"
        val badge2 = dateFmt.format(Date(document.issuedAt))
        c.drawRoundRect(40f, y - 12f, 40f + body.measureText(badge1) + 16f, y + 6f, 10f, 10f, stroke)
        c.drawText(badge1, 48f, y, muted)
        val b2x = 56f + body.measureText(badge1) + 16f
        c.drawRoundRect(b2x, y - 12f, b2x + body.measureText(badge2) + 16f, y + 6f, 10f, 10f, stroke)
        c.drawText(badge2, b2x + 8f, y, muted)
        y += 28f
        c.drawLine(40f, y, 555f, y, stroke)
        y += 20f

        // Company left / Client right
        val leftX = 40f
        val rightX = 320f
        var ly = y
        var ry = y
        c.drawText(company.name.ifBlank { "Cortot Élite" }.uppercase(Locale.FRANCE), leftX, ly, bold)
        ly += 14f
        if (company.legalName.isNotBlank()) { c.drawText(company.legalName, leftX, ly, muted); ly += 12f }
        if (company.address.isNotBlank()) {
            company.address.split("\n").forEach { c.drawText(it, leftX, ly, muted); ly += 12f }
        }
        if (company.phone.isNotBlank()) { c.drawText(company.phone, leftX, ly, muted); ly += 12f }
        if (company.email.isNotBlank()) { c.drawText(company.email, leftX, ly, muted); ly += 12f }
        if (company.siret.isNotBlank()) { c.drawText("SIRET ${company.siret}", leftX, ly, muted); ly += 12f }
        if (company.tvaNumber.isNotBlank()) { c.drawText("TVA ${company.tvaNumber}", leftX, ly, muted); ly += 12f }

        c.drawText("À L'ATTENTION DE", rightX, ry, bold)
        ry += 14f
        c.drawText(clientName(client), rightX, ry, body)
        ry += 12f
        client?.billingAddress?.takeIf { it.isNotBlank() }?.split("\n")?.forEach {
            c.drawText(it, rightX, ry, muted); ry += 12f
        }
        client?.siteAddress?.takeIf { it.isNotBlank() && it != client.billingAddress }?.let {
            c.drawText("Chantier : $it", rightX, ry, muted); ry += 12f
        }
        client?.phoneMobile?.takeIf { it.isNotBlank() }?.let {
            c.drawText(it, rightX, ry, muted); ry += 12f
        }
        client?.email?.takeIf { it.isNotBlank() }?.let {
            c.drawText(it, rightX, ry, muted); ry += 12f
        }

        y = maxOf(ly, ry) + 20f
        if (document.title.isNotBlank()) {
            c.drawText("Objet : ${document.title}", leftX, y, body)
            y += 18f
        }

        // Table header
        val colDesc = 40f
        val colPrix = 320f
        val colQty = 400f
        val colTot = 480f
        val rowH = 22f
        c.drawRect(40f, y, 555f, y + rowH, fillBlack)
        c.drawText("DESCRIPTION", colDesc + 6f, y + 15f, whiteBold)
        c.drawText("PRIX", colPrix, y + 15f, whiteBold)
        c.drawText("QTÉ", colQty, y + 15f, whiteBold)
        c.drawText("TOTAL", colTot, y + 15f, whiteBold)
        y += rowH

        lines.forEachIndexed { index, line ->
            val ht = (line.quantity * line.unitPriceHt).round2()
            val bg = if (index % 2 == 0) Color.rgb(248, 248, 248) else Color.WHITE
            val bgPaint = Paint().apply { color = bg; style = Paint.Style.FILL }
            // multi-line label support (first line only in cell for space)
            val labelLines = line.label.split("\n")
            val blockH = rowH + if (labelLines.size > 1) 12f else 0f
            c.drawRect(40f, y, 555f, y + blockH, bgPaint)
            c.drawLine(40f, y + blockH, 555f, y + blockH, stroke)
            c.drawText(labelLines.first().take(42), colDesc + 6f, y + 15f, body)
            if (labelLines.size > 1) {
                c.drawText(labelLines.getOrNull(1)?.take(50).orEmpty(), colDesc + 6f, y + 27f, muted)
            }
            c.drawText(line.unitPriceHt.euro(), colPrix, y + 15f, body)
            c.drawText(formatQty(line.quantity), colQty, y + 15f, body)
            c.drawText(ht.euro(), colTot, y + 15f, body)
            y += blockH
            if (y > 680f) return@forEachIndexed
        }

        y += 16f
        // Totals right-aligned
        fun totalLine(label: String, value: String, paint: Paint = body) {
            c.drawText(label, 360f, y, paint)
            c.drawText(value, 480f, y, paint)
            y += 16f
        }
        if (totals.discountPercent > 0) {
            totalLine("HT brut :", totals.htBrut.euro(), muted)
            totalLine("Remise ${totals.discountPercent}% :", "-${totals.discountHt.euro()}", muted)
        }
        totalLine("Sous-total HT :", totals.ht.euro())
        if (totals.vatDiscountPercent > 0) {
            totalLine("Remise TVA ${totals.vatDiscountPercent}% :", "-${totals.vatDiscount.euro()}", muted)
        }
        totalLine("TVA (${totals.rate}%) :", totals.vat.euro())
        y += 4f
        c.drawRect(350f, y - 14f, 555f, y + 10f, fillTotal)
        whiteBold.textSize = 12f
        c.drawText("TOTAL TTC :", 360f, y + 2f, whiteBold)
        c.drawText(totals.ttc.euro(), 470f, y + 2f, whiteBold)
        y += 36f

        // Footer
        c.drawLine(40f, y, 555f, y, stroke)
        y += 16f
        c.drawText("Paiement à l'ordre de ${company.name.ifBlank { "Cortot Élite" }}", 40f, y, muted)
        c.drawText("Conditions : paiement sous 30 jours", 340f, y, muted)
        y += 14f
        if (company.siret.isNotBlank()) {
            c.drawText("SIRET ${company.siret}  ·  TVA ${company.tvaNumber.ifBlank { "—" }}", 40f, y, muted)
            y += 14f
        }
        c.drawText("Pénalités de retard au taux légal. Pas d'escompte pour paiement anticipé.", 40f, y, muted)
        y += 24f
        val thanks = "MERCI DE VOTRE CONFIANCE"
        val tw = bold.measureText(thanks)
        c.drawText(thanks, (595f - tw) / 2f, y, bold)

        pdf.finishPage(page)
        val dir = File(context.cacheDir, "docs").apply { mkdirs() }
        val file = File(dir, "${document.number.replace('/', '-')}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return file
    }

    fun sharePdf(context: Context, file: File, subject: String, email: String? = null, body: String? = null) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            if (!email.isNullOrBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            if (!body.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Envoyer / enregistrer"))
    }

    fun emailDocument(
        context: Context,
        company: CompanySettings,
        client: Client?,
        document: Document,
        lines: List<DocumentLine>,
        totals: VatBreakdown
    ) {
        val email = client?.email?.trim().orEmpty()
        if (email.isBlank()) return
        val file = buildPdf(context, company, client, document, lines, totals)
        val isDevis = document.kind == DocumentKind.DEVIS
        val kind = if (isDevis) "devis" else "facture"
        val subject = if (isDevis) {
            "Devis ${document.number} — ${company.name.ifBlank { "Cortot Élite" }}"
        } else {
            "Facture ${document.number} — ${company.name.ifBlank { "Cortot Élite" }}"
        }
        val clientLabel = clientName(client)
        val body = if (isDevis) {
            """
            Bonjour $clientLabel,

            Veuillez trouver ci-joint notre devis n°${document.number} concernant : ${document.title.ifBlank { "entretien PV et contrôle" }}.

            Montant HT : ${totals.ht.euro()}
            TVA (${totals.rate} %) : ${totals.vat.euro()}
            Total TTC : ${totals.ttc.euro()}

            Restant à votre disposition pour toute question.

            Cordialement,
            ${company.name.ifBlank { "Cortot Élite" }}
            ${company.phone}
            ${company.email}
            """.trimIndent()
        } else {
            """
            Bonjour $clientLabel,

            Veuillez trouver ci-joint la facture n°${document.number}.

            Montant HT : ${totals.ht.euro()}
            TVA (${totals.rate} %) : ${totals.vat.euro()}
            Total TTC : ${totals.ttc.euro()}

            Paiement à l'ordre de ${company.name.ifBlank { "Cortot Élite" }}, sous 30 jours.

            Cordialement,
            ${company.name.ifBlank { "Cortot Élite" }}
            ${company.phone}
            ${company.email}
            """.trimIndent()
        }
        sharePdf(context, file, subject, email, body)
    }

    fun dial(context: Context, phone: String) {
        val cleaned = phone.filter { it.isDigit() || it == '+' }
        if (cleaned.isBlank()) return
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")))
    }

    private fun clientName(c: Client?): String {
        if (c == null) return "Client"
        return if (c.companyName.isNotBlank()) c.companyName
        else "${c.firstName} ${c.lastName}".trim().ifBlank { "Client" }
    }

    private fun formatQty(q: Double): String =
        if (q == q.toLong().toDouble()) q.toLong().toString() else q.toString()
}
