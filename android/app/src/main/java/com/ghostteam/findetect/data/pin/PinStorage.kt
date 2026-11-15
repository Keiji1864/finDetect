package com.ghostteam.findetect.data.pin

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.ghostteam.findetect.domain.repository.PinRepository
import com.ghostteam.findetect.domain.repository.PinResult
import com.ghostteam.findetect.domain.repository.PinStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Salted verifier and attempt limits are encrypted with a non-exportable Android Keystore key. */
@Singleton
class PinStorage @Inject constructor(@ApplicationContext context: Context) : PinRepository {
    private val preferences = context.getSharedPreferences("pin_security", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val mutableStatus = MutableStateFlow(PinStatus.LOADING)
    override val status = mutableStatus.asStateFlow()

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        mutex.withLock { readSafely(); Unit }
    }

    override suspend fun create(pin: String): PinResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            requireValid(pin)
            readSafely()
            if (mutableStatus.value != PinStatus.DISABLED) return@withLock PinResult.StorageError
            writeSafely(newRecord(pin), allowNewKey = true)
        }
    }

    override suspend fun verify(pin: String): PinResult = withContext(Dispatchers.IO) {
        mutex.withLock { verifyPin(pin) }
    }

    override suspend fun change(oldPin: String, newPin: String): PinResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            requireValid(newPin)
            val result = verifyPin(oldPin)
            if (result != PinResult.Success) return@withLock result
            writeSafely(newRecord(newPin))
        }
    }

    override suspend fun disable(pin: String): PinResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val result = verifyPin(pin)
            if (result != PinResult.Success) return@withLock result
            try {
                check(preferences.edit().remove(RECORD).remove(CONFIGURED).commit())
                deleteKey()
                mutableStatus.value = PinStatus.DISABLED
                PinResult.Success
            } catch (_: Exception) {
                mutableStatus.value = PinStatus.ERROR
                PinResult.StorageError
            }
        }
    }

    private fun verifyPin(pin: String): PinResult {
        requireValid(pin)
        val record = readSafely() ?: return PinResult.StorageError
        val now = System.currentTimeMillis()
        if (record.blockedUntil > now) {
            // A manual clock rollback must not turn a 30-second pause into a permanent lockout.
            if (record.blockedUntil - now > 30_000 && writeSafely(record.copy(blockedUntil = now + 30_000)) != PinResult.Success) {
                return PinResult.StorageError
            }
            return PinResult.Throttled(((record.blockedUntil - now + 999) / 1000).coerceAtMost(30).toInt())
        }
        val correct = MessageDigest.isEqual(record.verifier, verifier(pin, record.salt))
        val failures = if (record.blockedUntil != 0L) 0 else record.failures
        val next = if (correct) record.copy(failures = 0, blockedUntil = 0) else record.copy(
            failures = failures + 1,
            blockedUntil = if (failures + 1 >= 5) now + 30_000 else 0,
        )
        if (writeSafely(next) != PinResult.Success) return PinResult.StorageError
        return if (correct) PinResult.Success else if (next.blockedUntil > now) PinResult.Throttled(30) else PinResult.Incorrect
    }

    private fun readSafely(): Record? = try {
        val payload = preferences.getString(RECORD, null)
        val configured = preferences.getBoolean(CONFIGURED, false)
        if (payload == null && !configured) {
            // Empty/corrupt preferences must not disable a PIN whose Keystore key still exists.
            check(!keyStore().containsAlias(KEY_ALIAS))
            mutableStatus.value = PinStatus.DISABLED
            null
        } else {
            check(configured && payload != null)
            val bytes = Base64.decode(payload, Base64.NO_WRAP)
            check(bytes.size > 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(false), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            val json = JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
            check(json.getInt("version") == 1)
            val record = Record(
                Base64.decode(json.getString("salt"), Base64.NO_WRAP),
                Base64.decode(json.getString("verifier"), Base64.NO_WRAP),
                json.getInt("failures"), json.getLong("blockedUntil"),
            )
            check(record.salt.size == 16 && record.verifier.size == 32 && record.failures >= 0)
            mutableStatus.value = PinStatus.ENABLED
            record
        }
    } catch (_: Exception) {
        // Never replace a missing/invalid key or corrupt record with an unlocked default.
        mutableStatus.value = PinStatus.ERROR
        null
    }

    private fun writeSafely(record: Record, allowNewKey: Boolean = false): PinResult = try {
        val json = JSONObject().put("version", 1)
            .put("salt", Base64.encodeToString(record.salt, Base64.NO_WRAP))
            .put("verifier", Base64.encodeToString(record.verifier, Base64.NO_WRAP))
            .put("failures", record.failures).put("blockedUntil", record.blockedUntil)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(allowNewKey))
        val payload = Base64.encodeToString(cipher.iv + cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        check(preferences.edit().putString(RECORD, payload).putBoolean(CONFIGURED, true).commit())
        mutableStatus.value = PinStatus.ENABLED
        PinResult.Success
    } catch (_: Exception) {
        if (allowNewKey) {
            // A failed first save must not leave an orphan key or a partially configured PIN.
            try {
                check(preferences.edit().remove(RECORD).remove(CONFIGURED).commit())
                deleteKey()
                mutableStatus.value = PinStatus.DISABLED
            } catch (_: Exception) { mutableStatus.value = PinStatus.ERROR }
        }
        PinResult.StorageError
    }

    private fun key(allowCreate: Boolean): SecretKey {
        val store = keyStore()
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        check(allowCreate)
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
            generateKey()
        }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun deleteKey() = keyStore().deleteEntry(KEY_ALIAS)

    private fun newRecord(pin: String): Record {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return Record(salt, verifier(pin, salt), 0, 0)
    }

    private fun verifier(pin: String, salt: ByteArray): ByteArray {
        val chars = pin.toCharArray()
        val spec = PBEKeySpec(chars, salt, 100_000, 256)
        chars.fill('\u0000')
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }

    private fun requireValid(pin: String) { require(pin.length == 4 && pin.all { it in '0'..'9' }) }
    private data class Record(val salt: ByteArray, val verifier: ByteArray, val failures: Int, val blockedUntil: Long)
    private companion object {
        const val KEY_ALIAS = "findetect.pin.v1"
        const val CONFIGURED = "configured"
        const val RECORD = "encrypted_record"
    }
}

