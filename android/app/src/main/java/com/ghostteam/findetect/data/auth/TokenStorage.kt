package com.ghostteam.findetect.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.ghostteam.findetect.domain.model.SessionUser
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class StoredSession(
    val version: Int = 1,
    val mode: String,
    val user: SessionUser? = null,
    val sessionId: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAt: Long? = null,
    val cleanupUserId: String? = null,
    val resetPinUserId: String? = null,
)

/** One atomic encrypted record contains both rotating tokens. No password or plaintext backup. */
@Singleton
class TokenStorage @Inject constructor(@ApplicationContext context: Context, private val json: Json) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "session.v1"))
    private val mutex = Mutex()

    internal suspend fun read(): StoredSession? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val exists = file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()
            if (!exists) {
                check(!keyStore().containsAlias(ALIAS)) { "Session record is missing" }
                return@withLock null
            }
            val bytes = file.openRead().use { it.readBytes() }
            check(bytes.size > 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(false), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            json.decodeFromString<StoredSession>(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
                .also { check(it.version == 1 && it.mode in setOf("active", "signed_out", "reauth")) }
        }
    }

    /** Key replacement is permitted only after successful online authentication. */
    internal suspend fun write(record: StoredSession, recoverAfterAuthentication: Boolean = false) = withContext(Dispatchers.IO) {
        mutex.withLock {
            var cipher = Cipher.getInstance("AES/GCM/NoPadding")
            try {
                cipher.init(Cipher.ENCRYPT_MODE, key(true))
            } catch (error: Exception) {
                if (!recoverAfterAuthentication) throw error
                keyStore().deleteEntry(ALIAS)
                cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, key(true))
            }
            val encrypted = cipher.iv + cipher.doFinal(json.encodeToString(record).toByteArray(Charsets.UTF_8))
            val output = file.startWrite()
            try {
                output.write(encrypted)
                file.finishWrite(output)
            } catch (error: Exception) {
                file.failWrite(output)
                throw error
            }
        }
    }

    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun key(create: Boolean): SecretKey {
        val store = keyStore()
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        check(create && !store.containsAlias(ALIAS)) { "Session key unavailable" }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setKeySize(256)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    private companion object { const val ALIAS = "findetect.session.v1" }
}

