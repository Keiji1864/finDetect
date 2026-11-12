package com.ghostteam.findetect.data.local.migration

import android.database.Cursor
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ghostteam.findetect.data.local.Converters
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.datetime.Instant

/** Room runs this migration in a transaction; an invalid legacy row rolls back the whole upgrade. */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf("transactions", "balance_adjustments", "accounts", "categories").forEach { table ->
            db.execSQL("ALTER TABLE `$table` RENAME TO `${table}_v1`")
        }

        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `accounts` (
                `id` TEXT NOT NULL, `name` TEXT NOT NULL, `iconId` TEXT NOT NULL,
                `currency` TEXT NOT NULL, `initialBalanceMinor` INTEGER NOT NULL,
                `modifiedAt` TEXT NOT NULL, `includeInForecast` INTEGER NOT NULL,
                `createdAt` TEXT, `updatedAt` TEXT, `deletedAt` TEXT,
                `syncStatus` TEXT NOT NULL, `legacyEmoji` TEXT, PRIMARY KEY(`id`))""",
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `categories` (
                `id` TEXT NOT NULL, `key` TEXT NOT NULL, `nameRu` TEXT NOT NULL,
                `nameEn` TEXT NOT NULL, `iconId` TEXT NOT NULL, `isIncome` INTEGER NOT NULL,
                `isArchived` INTEGER NOT NULL, `legacyEmoji` TEXT, PRIMARY KEY(`id`))""",
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `transactions` (
                `id` TEXT NOT NULL, `accountId` TEXT NOT NULL, `categoryId` TEXT NOT NULL,
                `amountMinor` INTEGER NOT NULL, `transactionDate` TEXT NOT NULL,
                `enteredAt` TEXT NOT NULL, `modifiedAt` TEXT NOT NULL, `comment` TEXT,
                `createdAt` TEXT, `updatedAt` TEXT, `deletedAt` TEXT,
                `syncStatus` TEXT NOT NULL, PRIMARY KEY(`id`),
                FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)""",
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `balance_adjustments` (
                `id` TEXT NOT NULL, `accountId` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL,
                `occurredAt` TEXT NOT NULL, `createdAt` TEXT, `syncStatus` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)""",
        )

        db.query("SELECT * FROM accounts_v1").use { cursor ->
            while (cursor.moveToNext()) {
                val updatedAt = cursor.timestamp("updatedAt")
                val emoji = cursor.text("emoji")
                db.execSQL(
                    """INSERT INTO accounts (id, name, iconId, currency, initialBalanceMinor,
                        modifiedAt, includeInForecast, createdAt, updatedAt, deletedAt, syncStatus, legacyEmoji)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    arrayOf(
                        cursor.text("id"), cursor.text("name"), legacyIconId(emoji),
                        cursor.text("currency"), cursor.moneyMinor("initialBalance"), updatedAt,
                        cursor.number("includeInForecast"), cursor.timestamp("createdAt"), updatedAt,
                        if (cursor.isDeleted()) updatedAt else null, cursor.text("syncStatus"), emoji,
                    ),
                )
            }
        }
        db.query("SELECT * FROM categories_v1").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.text("id")
                val name = cursor.text("name")
                val emoji = cursor.text("emoji")
                db.execSQL(
                    """INSERT INTO categories (id, `key`, nameRu, nameEn, iconId, isIncome, isArchived, legacyEmoji)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                    arrayOf(id, "legacy:$id", name, name, legacyIconId(emoji), cursor.number("isIncome"), 0, emoji),
                )
            }
        }
        db.query("SELECT * FROM transactions_v1").use { cursor ->
            while (cursor.moveToNext()) {
                val updatedAt = cursor.timestamp("updatedAt")
                db.execSQL(
                    """INSERT INTO transactions (id, accountId, categoryId, amountMinor, transactionDate,
                        enteredAt, modifiedAt, comment, createdAt, updatedAt, deletedAt, syncStatus)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    arrayOf(
                        cursor.text("id"), cursor.text("accountId"), cursor.text("categoryId"),
                        cursor.moneyMinor("amount"), cursor.timestamp("occurredAt"), cursor.timestamp("enteredAt"),
                        updatedAt, cursor.nullableText("title"), cursor.timestamp("createdAt"), updatedAt,
                        if (cursor.isDeleted()) updatedAt else null, cursor.text("syncStatus"),
                    ),
                )
            }
        }
        db.query("SELECT * FROM balance_adjustments_v1").use { cursor ->
            while (cursor.moveToNext()) {
                val createdAt = cursor.timestamp("createdAt")
                db.execSQL(
                    """INSERT INTO balance_adjustments (id, accountId, amountMinor, occurredAt, createdAt, syncStatus)
                        VALUES (?, ?, ?, ?, ?, ?)""",
                    arrayOf(
                        cursor.text("id"), cursor.text("accountId"), cursor.moneyMinor("amount"),
                        createdAt, createdAt, cursor.text("syncStatus"),
                    ),
                )
            }
        }

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_transactionDate` ON `transactions` (`transactionDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_balance_adjustments_accountId` ON `balance_adjustments` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_balance_adjustments_createdAt` ON `balance_adjustments` (`createdAt`)")

        // v1 had no foreign keys. Never silently drop an orphan while adding referential integrity.
        db.query("PRAGMA foreign_key_check").use { cursor ->
            check(!cursor.moveToFirst()) { "Financial v1 database contains orphaned references" }
        }
        listOf("transactions", "balance_adjustments", "accounts", "categories").forEach { table ->
            db.execSQL("DROP TABLE `${table}_v1`")
        }
    }
}

private fun Cursor.text(column: String): String = getString(getColumnIndexOrThrow(column))

private fun Cursor.nullableText(column: String): String? =
    getColumnIndexOrThrow(column).let { if (isNull(it)) null else getString(it) }

private fun Cursor.number(column: String): Long = getLong(getColumnIndexOrThrow(column))

private fun Cursor.timestamp(column: String): String =
    checkNotNull(Converters().fromInstant(Instant.fromEpochMilliseconds(number(column))))

private fun Cursor.isDeleted(): Boolean = number("isDeleted") != 0L || text("syncStatus") == "PENDING_DELETE"

// v1 REAL values already have binary approximation. Round their decimal representation once,
// without multiplying a Double or truncating fractional kopecks in SQLite.
private fun Cursor.moneyMinor(column: String): Long =
    BigDecimal(text(column)).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()

private val legacyIconKeys = setOf(
    "wallet", "chart", "user", "gear", "basket", "coffee", "bus", "cash",
    "calendar", "trophy", "flame", "spark", "lock", "receipt", "plus", "check",
)

private fun legacyIconId(emoji: String): String = if (emoji in legacyIconKeys) emoji else "wallet"
