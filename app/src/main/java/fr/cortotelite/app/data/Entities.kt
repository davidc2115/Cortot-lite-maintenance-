package fr.cortotelite.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class ClientType { PARTICULIER, PROFESSIONNEL }
enum class DocumentKind { DEVIS, FACTURE }
enum class DocumentStatus { BROUILLON, ENVOYE, ACCEPTE, REFUSE, PAYE }
enum class TravelMode { FORFAIT, KM, TEMPS }

@Entity(tableName = "company")
data class CompanySettings(
    @PrimaryKey val id: Int = 1,
    val name: String = "Cortot Élite",
    val legalName: String = "",
    val address: String = "",
    val siret: String = "",
    val tvaNumber: String = "",
    val phone: String = "",
    val email: String = "",
    val defaultVatRate: Double = 20.0,
    val reducedVatRate: Double = 10.0,
    val travelRatePerKmHt: Double = 0.80,
    val travelForfaitHt: Double = 45.0,
    val travelHourlyHt: Double = 55.0,
    /** Si true, le calcul auto propose l'aller-retour (km × 2). */
    val travelRoundTripDefault: Boolean = true,
    /** Prix HT par kWc pour facturation auto de l'installation. */
    val pricePerKwcHt: Double = 40.0,
    val nextQuoteNumber: Int = 1,
    val nextInvoiceNumber: Int = 1
)

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ClientType = ClientType.PARTICULIER,
    val lastName: String = "",
    val firstName: String = "",
    val companyName: String = "",
    val billingAddress: String = "",
    val siteAddress: String = "",
    val phoneLandline: String = "",
    val phoneMobile: String = "",
    val email: String = "",
    val notes: String = "",
    /** Distance aller simple société → chantier (km), mise en cache. 0 = inconnu. */
    val distanceKm: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "installations",
    foreignKeys = [ForeignKey(
        entity = Client::class,
        parentColumns = ["id"],
        childColumns = ["clientId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("clientId")]
)
data class Installation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val powerKwc: Double = 0.0,
    val inverterBrand: String = "",
    val inverterModel: String = "",
    val inverterCount: Int = 1,
    val panelBrand: String = "",
    val panelCount: Int = 0,
    val needsLadder: Boolean = false,
    val roofType: String = "",
    val accessNotes: String = ""
)

@Entity(
    tableName = "documents",
    foreignKeys = [ForeignKey(
        entity = Client::class,
        parentColumns = ["id"],
        childColumns = ["clientId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("clientId")]
)
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val kind: DocumentKind,
    val number: String,
    val status: DocumentStatus = DocumentStatus.BROUILLON,
    val title: String = "",
    val vatRate: Double = 20.0,
    /** Remise commerciale % sur le total HT. */
    val discountPercent: Double = 0.0,
    /** Remise % sur le montant de TVA. */
    val vatDiscountPercent: Double = 0.0,
    val issuedAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val convertedFromQuoteId: Long? = null
)

@Entity(
    tableName = "document_lines",
    foreignKeys = [ForeignKey(
        entity = Document::class,
        parentColumns = ["id"],
        childColumns = ["documentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("documentId")]
)
data class DocumentLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val label: String,
    val quantity: Double = 1.0,
    val unitPriceHt: Double = 0.0,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "travels",
    foreignKeys = [
        ForeignKey(entity = Client::class, parentColumns = ["id"], childColumns = ["clientId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Document::class, parentColumns = ["id"], childColumns = ["documentId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("clientId"), Index("documentId")]
)
data class Travel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val documentId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val mode: TravelMode = TravelMode.KM,
    val kilometers: Double = 0.0,
    val hours: Double = 0.0,
    val amountHt: Double = 0.0,
    val vatRate: Double = 20.0,
    val comment: String = ""
)

data class ClientWithInstall(
    @Embedded val client: Client,
    @Relation(parentColumn = "id", entityColumn = "clientId")
    val installations: List<Installation>
)

data class DocumentWithLines(
    @Embedded val document: Document,
    @Relation(parentColumn = "id", entityColumn = "documentId")
    val lines: List<DocumentLine>
)
