package com.ghostteam.findetect.data.demo

import android.content.Context
import com.ghostteam.findetect.domain.model.SessionUser
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Minimal debug server state: profile identity/name and deletion, independent of client logout. No passwords/tokens. */
@Singleton
class DebugProfileStore @Inject constructor(@ApplicationContext context: Context, private val json: Json) {
    private val preferences = context.getSharedPreferences("debug_profile_server_v1", Context.MODE_PRIVATE)
    private val mutex = Mutex()

    @Serializable
    private data class Account(val user: SessionUser, val deleted: Boolean = false)
    @Serializable
    private data class Server(val accounts: Map<String, Account> = emptyMap(), val retiredIds: Set<String> = emptySet(),
        val deletedEmails: Set<String> = emptySet())

    suspend fun login(email: String, defaultName: String): SessionUser? = access { server ->
        val key = email.trim().lowercase()
        if (emailHash(key) in server.deletedEmails) return@access null
        val existing = server.accounts[key]
        if (existing != null) return@access existing.user.takeUnless { existing.deleted }
        val user = SessionUser(initialId(key), key, defaultName)
        write(server.copy(accounts = server.accounts + (key to Account(user))))
        user
    }

    suspend fun register(email: String, name: String): SessionUser? = access { server ->
        val key = email.trim().lowercase()
        val existing = server.accounts[key]
        if (existing != null && !existing.deleted) return@access null
        val user = SessionUser(UUID.randomUUID().toString(), key, name.trim())
        write(server.copy(accounts = server.accounts + (key to Account(user)), deletedEmails = server.deletedEmails - emailHash(key)))
        user
    }

    suspend fun authorize(tokenUser: SessionUser): SessionUser? = access { server ->
        val key = tokenUser.email.trim().lowercase()
        if (tokenUser.id in server.retiredIds) return@access null
        if (emailHash(key) in server.deletedEmails) return@access null
        val existing = server.accounts[key]
        if (existing != null) return@access existing.user.takeIf { !existing.deleted && it.id == tokenUser.id }
        // Adopt tokens issued by the previous stateless fixtures without resetting their user ID/name.
        val user = tokenUser.copy(email = key)
        write(server.copy(accounts = server.accounts + (key to Account(user))))
        user
    }

    suspend fun updateName(userId: String, name: String): SessionUser? = access { server ->
        val entry = server.accounts.entries.firstOrNull { !it.value.deleted && it.value.user.id == userId } ?: return@access null
        val user = entry.value.user.copy(name = name)
        write(server.copy(accounts = server.accounts + (entry.key to Account(user))))
        user
    }

    suspend fun delete(userId: String): Boolean = access { server ->
        val entry = server.accounts.entries.firstOrNull { !it.value.deleted && it.value.user.id == userId } ?: return@access false
        write(server.copy(accounts = server.accounts - entry.key, retiredIds = server.retiredIds + userId,
            deletedEmails = server.deletedEmails + emailHash(entry.key)))
        true
    }

    private suspend fun <T> access(block: (Server) -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock {
            val stored = preferences.getString("server", null)
            block(if (stored == null) Server() else json.decodeFromString<Server>(stored))
        }
    }
    private fun write(server: Server) { check(preferences.edit().putString("server", json.encodeToString(server)).commit()) }
    private fun emailHash(email: String): String = MessageDigest.getInstance("SHA-256").digest(email.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    companion object {
        fun initialId(email: String): String = UUID.nameUUIDFromBytes(email.toByteArray(Charsets.UTF_8)).toString()
    }
}
