package fr.cortotelite.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
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
}

@Database(
    entities = [
        CompanySettings::class, Client::class, Installation::class,
        Document::class, DocumentLine::class, Travel::class
    ],
    version = 1,
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
                ).addCallback(object : Callback() {
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
