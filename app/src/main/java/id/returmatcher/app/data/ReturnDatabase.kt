package id.returmatcher.app.data

import android.content.Context
import androidx.room.*

@Entity(tableName = "master_orders", indices = [Index("orderNumber"), Index("trackingNumber")])
data class MasterOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNumber: String,
    val trackingNumber: String,
    val marketplace: String = "",
    val product: String = "",
    val importedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "scanned_returns", indices = [Index("scannedCode")])
data class ScannedReturn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scannedCode: String,
    val matchedOrderId: Long? = null,
    val result: String,
    val scannedAt: Long = System.currentTimeMillis()
)

@Dao interface ReturnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMaster(items: List<MasterOrder>)
    @Query("DELETE FROM master_orders") suspend fun clearMaster()
    @Query("SELECT * FROM master_orders WHERE orderNumber = :code OR trackingNumber = :code LIMIT 1") suspend fun findMaster(code: String): MasterOrder?
    @Query("SELECT COUNT(*) FROM master_orders") suspend fun masterCount(): Int
    @Query("SELECT * FROM master_orders ORDER BY id DESC") suspend fun allMaster(): List<MasterOrder>
    @Query("SELECT * FROM scanned_returns ORDER BY scannedAt DESC") suspend fun allScans(): List<ScannedReturn>
    @Query("SELECT COUNT(*) FROM scanned_returns WHERE scannedCode = :code") suspend fun scanCount(code: String): Int
    @Insert suspend fun insertScan(item: ScannedReturn)
    @Query("SELECT COUNT(*) FROM scanned_returns WHERE result = 'COCOK'") suspend fun matchedCount(): Int
    @Query("SELECT COUNT(*) FROM scanned_returns WHERE result = 'TIDAK_DITEMUKAN'") suspend fun missingCount(): Int
}

@Database(entities = [MasterOrder::class, ScannedReturn::class], version = 1, exportSchema = false)
abstract class ReturnDatabase : RoomDatabase() {
    abstract fun dao(): ReturnDao
    companion object {
        @Volatile private var INSTANCE: ReturnDatabase? = null
        fun get(context: Context): ReturnDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, ReturnDatabase::class.java, "retur_matcher.db").build().also { INSTANCE = it }
        }
    }
}
