package com.ghostteam.findetect.data.sync

import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ghostteam.findetect.data.local.AppDatabase
import com.ghostteam.findetect.data.repository.ForecastHistory
import com.ghostteam.findetect.data.session.UserAccessGate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal typealias WireRow = Map<String, String?>

@Serializable
internal data class Changes(val revision: Long, val tables: Map<String, List<WireRow>>)

private data class Table(val name: String, val fields: String, val hasStatus: Boolean) {
    val columns = fields.split(" ")
    val key = columns.first()
    val projection = columns.joinToString { "`$it`" }
    fun id(row: WireRow) = row.getValue(key)!!
    fun signature(row: WireRow): String {
        val bytes = Json.encodeToString(columns.map { row[it] }).toByteArray()
        return android.util.Base64.encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(bytes),
            android.util.Base64.NO_WRAP)
    }
}

private data class Batch(val request: Changes, val observed: Map<String, Map<String, WireRow>>)

/** Per-login state. A network round trip never holds a Room transaction or the session gate. */
internal class RoomExchange(private val room: AppDatabase, private val gate: UserAccessGate, private val history: ForecastHistory) {
    private val serial = Mutex()
    private val db get() = room.openHelper.writableDatabase

    suspend fun awaitChange() {
        kotlinx.coroutines.withTimeoutOrNull(45_000) {
            room.invalidationTracker.createFlow(*tables.map { it.name }.toTypedArray(), emitInitialState = false).first()
        }
    }

    suspend fun pending(): Boolean = gate.use { room.withTransaction {
        val batch = capture()
        batch == null || batch.request.tables.values.any { it.isNotEmpty() }
    } }

    suspend fun exchange(send: suspend (Changes) -> Changes) = serial.withLock {
        val batch = gate.use { room.withTransaction { capture() } } ?: return@withLock
        val response = send(batch.request)
        require(response.revision >= batch.request.revision)
        gate.use { room.withTransaction {
            for (table in tables) {
                val incoming = response.tables.getValue(table.name)
                val before = batch.observed.getValue(table.name)
                for (row in incoming) {
                    require(row.keys == table.columns.toSet())
                    val id = table.id(row)
                    val current = readRows(db,table,id).firstOrNull()
                    // Preserve an edit committed while the request was in flight.
                    if (current == before[id] && (current != row || table.hasStatus)) applyRow(table,row,current != null)
                    db.execSQL("INSERT OR REPLACE INTO sync_shadow (section,recordId,content) VALUES(?,?,?)",
                        arrayOf(table.name,id,table.signature(row)))
                }
            }
            history.checkpoint()
            db.execSQL("INSERT OR REPLACE INTO sync_shadow VALUES('cursor','','' || ?)",arrayOf(response.revision))
        } }
    }

    private suspend fun capture(): Batch? {
        db.execSQL("""CREATE TABLE IF NOT EXISTS sync_shadow (
            section TEXT NOT NULL, recordId TEXT NOT NULL, content TEXT NOT NULL, PRIMARY KEY(section,recordId))""")
        val now = Clock.System.now()
        room.accountDeletionDao().releaseExpired(now)
        room.transactionDeletionDao().releaseExpired(now)
        val held = db.query("SELECT EXISTS(SELECT 1 FROM account_deletions) OR EXISTS(SELECT 1 FROM transaction_deletions)")
            .use { it.moveToFirst(); it.getInt(0) != 0 }
        if (held) return null
        val shadows = mutableMapOf<Pair<String,String>,String>()
        db.query("SELECT section,recordId,content FROM sync_shadow").use { cursor ->
            while (cursor.moveToNext()) shadows[cursor.getString(0) to cursor.getString(1)] = cursor.getString(2)
        }
        val observed = tables.associate { t -> t.name to readRows(db,t).associateBy(t::id) }
        val dirty = tables.associate { t -> t.name to observed.getValue(t.name).values.filter {
            shadows[t.name to t.id(it)] != t.signature(it)
        } }
        return Batch(Changes(shadows["cursor" to ""]?.toLong() ?: 0,dirty),observed)
    }

    private fun readRows(db: SupportSQLiteDatabase,t: Table,id: String? = null): List<WireRow> {
        val sql = "SELECT ${t.projection} FROM ${t.name}"
        val cursor = if (id==null) db.query(sql) else db.query("$sql WHERE `${t.key}`=?", arrayOf(id))
        return cursor.use { rows -> buildList {
            while (rows.moveToNext()) add(t.columns.mapIndexed { index,c ->
                c to if (rows.isNull(index)) null else rows.getString(index)
            }.toMap())
        } }
    }

    private fun applyRow(t: Table,row: WireRow,exists: Boolean) {
        val values = LinkedHashMap(row)
        if (t.hasStatus) values["syncStatus"]="SYNCED"
        if (exists) {
            values.remove(t.key)
            val assignments=values.keys.joinToString { "`$it`=?" }
            bindRow("UPDATE ${t.name} SET $assignments WHERE `${t.key}`=?", values.values + t.id(row))
        } else {
            val columns=values.keys.joinToString { "`$it`" }
            val placeholders=values.keys.joinToString { "?" }
            bindRow("INSERT INTO ${t.name} ($columns) VALUES ($placeholders)", values.values)
        }
    }

    private fun bindRow(sql: String, values: Collection<String?>) {
        db.compileStatement(sql).use { statement ->
            values.forEachIndexed { index, value ->
                if (value == null) statement.bindNull(index + 1) else statement.bindString(index + 1, value)
            }
            statement.execute()
        }
    }

    companion object {
        private val tables = listOf(
            Table("categories", "id key nameRu nameEn iconId isIncome isArchived legacyEmoji", false),
            Table("accounts", "id name iconId currency initialBalanceMinor modifiedAt includeInForecast createdAt updatedAt deletedAt legacyEmoji", true),
            Table("transactions", "id accountId categoryId amountMinor transactionDate enteredAt modifiedAt comment createdAt updatedAt deletedAt", true),
            Table("balance_adjustments", "id accountId amountMinor occurredAt createdAt", true),
            Table("balance_confirmations", "requestId accountId currency actualMinor adjustmentId", false),
            Table("activity_days", "date hadTransaction noSpendingConfirmed enteredAt modifiedAt", false),
            Table("xp_state", "id startedAt legacyBestStreak", false),
            Table("achievement_state", "id startedAt startedOn zoneId freshStartDate", false),
            Table("achievement_transaction_facts", "transactionId enteredOn transactionOn zoneId enteredAt", false),
            Table("xp_events", "id reason sourceId reasonRef amount counted localDate occurredAt createdAt", true),
            Table("achievements", "code earnedAt earnedOn isLegacy xpEventId createdAt", true),
            Table("forecast_days", "id currency day stamp accountIds fromUtc untilUtc limitMinor", false),
            Table("forecast_balances", "id currency stamp localDay targetDay rule accountIds balanceMinor", false),
            Table("personal_settings", "id gameEnabled reminderEnabled reminderHour modifiedAt nextIncomeDate incomeRepeat", false)
        )
    }
}
