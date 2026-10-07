package fr.cortotelite.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Converters {
    @TypeConverter fun fromClientType(v: ClientType) = v.name
    @TypeConverter fun toClientType(v: String) = ClientType.valueOf(v)
    @TypeConverter fun fromKind(v: DocumentKind) = v.name
    @TypeConverter fun toKind(v: String) = DocumentKind.valueOf(v)
    @TypeConverter fun fromStatus(v: DocumentStatus) = v.name
    @TypeConverter fun toStatus(v: String) = DocumentStatus.valueOf(v)
    @TypeConverter fun fromTravel(v: TravelMode) = v.name
    @TypeConverter fun toTravel(v: String) = TravelMode.valueOf(v)
    @TypeConverter fun fromMaint(v: MaintenanceBillingMode) = v.name
    @TypeConverter fun toMaint(v: String) = MaintenanceBillingMode.valueOf(v)
    @TypeConverter fun fromEmployeeRole(v: EmployeeRole) = v.name
    @TypeConverter fun toEmployeeRole(v: String) = EmployeeRole.valueOf(v)
    @TypeConverter fun fromPayslipStatus(v: PayslipStatus) = v.name
    @TypeConverter fun toPayslipStatus(v: String) = PayslipStatus.valueOf(v)
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE clients ADD COLUMN distanceKm REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE company ADD COLUMN travelRoundTripDefault INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE company ADD COLUMN pricePerKwcHt REAL NOT NULL DEFAULT 40.0")
        db.execSQL("ALTER TABLE documents ADD COLUMN discountPercent REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE documents ADD COLUMN vatDiscountPercent REAL NOT NULL DEFAULT 0.0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE company ADD COLUMN maintenanceBillingMode TEXT NOT NULL DEFAULT 'PUISSANCE'")
        db.execSQL("ALTER TABLE company ADD COLUMN maintenanceForfaitHt REAL NOT NULL DEFAULT 80.0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Nouveaux champs company
        db.execSQL("ALTER TABLE company ADD COLUMN paymentDelayDays INTEGER NOT NULL DEFAULT 30")
        db.execSQL("ALTER TABLE company ADD COLUMN commissionPercent REAL NOT NULL DEFAULT 10.0")
        db.execSQL("ALTER TABLE company ADD COLUMN geminiApiKeys TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE company ADD COLUMN geminiModel TEXT NOT NULL DEFAULT 'gemini-2.0-flash'")
        // Nouveaux champs documents
        db.execSQL("ALTER TABLE documents ADD COLUMN dueAt INTEGER")
        db.execSQL("ALTER TABLE documents ADD COLUMN paidAt INTEGER")
        // Tables employés / paie / dividendes
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS employees (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                lastName TEXT NOT NULL,
                firstName TEXT NOT NULL,
                role TEXT NOT NULL,
                email TEXT NOT NULL,
                phone TEXT NOT NULL,
                address TEXT NOT NULL,
                iban TEXT NOT NULL,
                socialSecurityNumber TEXT NOT NULL,
                hireDate INTEGER NOT NULL,
                baseSalaryBrut REAL NOT NULL,
                commissionPercent REAL NOT NULL,
                dividendPercent REAL NOT NULL,
                active INTEGER NOT NULL,
                notes TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS payslips (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                employeeId INTEGER NOT NULL,
                year INTEGER NOT NULL,
                month INTEGER NOT NULL,
                status TEXT NOT NULL,
                baseBrut REAL NOT NULL,
                commissionBrut REAL NOT NULL,
                primesBrut REAL NOT NULL,
                chargesSalariales REAL NOT NULL,
                netAPayer REAL NOT NULL,
                caPayeHt REAL NOT NULL,
                caEnAttenteHt REAL NOT NULL,
                caRetardHt REAL NOT NULL,
                notes TEXT NOT NULL,
                generatedAt INTEGER NOT NULL,
                paidAt INTEGER,
                FOREIGN KEY(employeeId) REFERENCES employees(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_payslips_employeeId ON payslips(employeeId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_payslips_employeeId_year_month ON payslips(employeeId, year, month)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS dividends (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                employeeId INTEGER NOT NULL,
                year INTEGER NOT NULL,
                amount REAL NOT NULL,
                percent REAL NOT NULL,
                baseCaHt REAL NOT NULL,
                notes TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                paidAt INTEGER,
                FOREIGN KEY(employeeId) REFERENCES employees(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_dividends_employeeId ON dividends(employeeId)")
    }
}

@Database(
    entities = [
        CompanySettings::class, Client::class, Installation::class,
        Document::class, DocumentLine::class, Travel::class,
        Employee::class, Payslip::class, Dividend::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var instance: AppDb? = null

        fun get(context: Context): AppDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDb::class.java,
                    "cortot_elite.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                get(context).dao().upsertCompany(CompanySettings())
                            }
                        }
                    }).build().also { instance = it }
            }
    }
}
