package fr.cortotelite.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM company WHERE id = 1")
    fun company(): Flow<CompanySettings?>

    @Query("SELECT * FROM company WHERE id = 1")
    suspend fun companyNow(): CompanySettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompany(settings: CompanySettings)

    @Query("SELECT * FROM clients ORDER BY lastName, firstName, companyName")
    fun clients(): Flow<List<Client>>

    @Transaction
    @Query("SELECT * FROM clients WHERE id = :id")
    fun clientWithInstall(id: Long): Flow<ClientWithInstall?>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun client(id: Long): Client?

    @Insert
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)

    @Insert
    suspend fun insertInstallation(installation: Installation): Long

    @Update
    suspend fun updateInstallation(installation: Installation)

    @Query("SELECT * FROM installations WHERE clientId = :clientId LIMIT 1")
    suspend fun installationFor(clientId: Long): Installation?

    @Query("SELECT * FROM documents ORDER BY issuedAt DESC")
    fun documents(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE clientId = :clientId ORDER BY issuedAt DESC")
    fun documentsFor(clientId: Long): Flow<List<Document>>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :id")
    fun documentWithLines(id: Long): Flow<DocumentWithLines?>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun document(id: Long): Document?

    @Insert
    suspend fun insertDocument(document: Document): Long

    @Update
    suspend fun updateDocument(document: Document)

    @Delete
    suspend fun deleteDocument(document: Document)

    @Query("SELECT * FROM document_lines WHERE documentId = :documentId ORDER BY sortOrder")
    suspend fun lines(documentId: Long): List<DocumentLine>

    @Insert
    suspend fun insertLine(line: DocumentLine): Long

    @Update
    suspend fun updateLine(line: DocumentLine)

    @Delete
    suspend fun deleteLine(line: DocumentLine)

    @Query("""
        SELECT * FROM documents
        WHERE kind = 'FACTURE'
          AND issuedAt >= :fromMs AND issuedAt < :toMs
        ORDER BY issuedAt
    """)
    suspend fun invoicesInPeriod(fromMs: Long, toMs: Long): List<Document>

    @Query("SELECT * FROM travels ORDER BY date DESC")
    fun travels(): Flow<List<Travel>>

    @Query("SELECT * FROM travels WHERE clientId = :clientId ORDER BY date DESC")
    fun travelsFor(clientId: Long): Flow<List<Travel>>

    @Query("SELECT * FROM travels WHERE documentId = :documentId")
    suspend fun travelsForDocument(documentId: Long): List<Travel>

    @Insert
    suspend fun insertTravel(travel: Travel): Long

    @Update
    suspend fun updateTravel(travel: Travel)

    @Delete
    suspend fun deleteTravel(travel: Travel)

    @Query("SELECT * FROM employees ORDER BY lastName, firstName")
    fun employees(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE active = 1 ORDER BY lastName, firstName")
    fun activeEmployees(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun employee(id: Long): Employee?

    @Transaction
    @Query("SELECT * FROM employees WHERE id = :id")
    fun employeeWithPayslips(id: Long): Flow<EmployeeWithPayslips?>

    @Insert
    suspend fun insertEmployee(employee: Employee): Long

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Delete
    suspend fun deleteEmployee(employee: Employee)

    @Query("SELECT * FROM payslips ORDER BY year DESC, month DESC")
    fun payslips(): Flow<List<Payslip>>

    @Query("SELECT * FROM payslips WHERE employeeId = :employeeId ORDER BY year DESC, month DESC")
    fun payslipsFor(employeeId: Long): Flow<List<Payslip>>

    @Query("SELECT * FROM payslips WHERE id = :id")
    suspend fun payslip(id: Long): Payslip?

    @Query("SELECT * FROM payslips WHERE employeeId = :employeeId AND year = :year AND month = :month LIMIT 1")
    suspend fun payslipForPeriod(employeeId: Long, year: Int, month: Int): Payslip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayslip(payslip: Payslip): Long

    @Update
    suspend fun updatePayslip(payslip: Payslip)

    @Delete
    suspend fun deletePayslip(payslip: Payslip)

    @Query("SELECT * FROM dividends ORDER BY year DESC, createdAt DESC")
    fun dividends(): Flow<List<Dividend>>

    @Query("SELECT * FROM dividends WHERE employeeId = :employeeId ORDER BY year DESC")
    fun dividendsFor(employeeId: Long): Flow<List<Dividend>>

    @Insert
    suspend fun insertDividend(dividend: Dividend): Long

    @Update
    suspend fun updateDividend(dividend: Dividend)

    @Delete
    suspend fun deleteDividend(dividend: Dividend)
}
