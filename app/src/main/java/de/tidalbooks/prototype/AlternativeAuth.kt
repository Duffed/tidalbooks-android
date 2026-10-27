package de.tidalbooks.prototype

import android.content.Context
import android.os.SystemClock
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.tidal.sdk.auth.CredentialsProvider
import com.tidal.sdk.auth.model.AuthResult
import com.tidal.sdk.auth.model.Credentials
import com.tidal.sdk.auth.model.CredentialsUpdatedMessage
import com.tidal.sdk.common.TidalMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.security.KeyStore
import java.io.IOException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Independent user-authorized device session. The desktop never receives its tokens. */
class AlternativeAuth(context: Context) : CredentialsProvider {
    private val preferences = context.getSharedPreferences("alternative-auth", Context.MODE_PRIVATE)
    private val http = OkHttpClient.Builder().followRedirects(false).build()
    private val lock = Mutex()
    override val bus = MutableSharedFlow<TidalMessage>(extraBufferCapacity = 4)
    var selected: Boolean
        get() = preferences.getBoolean("selected", false)
        set(value) { preferences.edit().putBoolean("selected", value).apply() }
    private val keyAlias = "de.tidalbooks.alternative.session"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }
    private fun saved(): JSONObject? = runCatching {
        val encoded = preferences.getString("session", null) ?: return null
        val envelope = JSONObject(encoded)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)))
        JSONObject(String(cipher.doFinal(Base64.decode(envelope.getString("data"), Base64.NO_WRAP)), Charsets.UTF_8))
    }.getOrNull()
    private fun save(session: JSONObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(session.toString().toByteArray(Charsets.UTF_8))
        val envelope = JSONObject().put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        check(preferences.edit().putString("session", envelope.toString()).commit()) { "Anmeldung konnte nicht sicher gespeichert werden." }
    }
    private fun verifySecureStorage() {
        // Check the actual device keystore before requesting any account authorization.
        val secretKey = key()
        val encrypt = Cipher.getInstance("AES/GCM/NoPadding")
        encrypt.init(Cipher.ENCRYPT_MODE, secretKey)
        val ciphertext = encrypt.doFinal("tidalbooks-storage-check".toByteArray(Charsets.UTF_8))
        val decrypt = Cipher.getInstance("AES/GCM/NoPadding")
        decrypt.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, encrypt.iv))
        check(String(decrypt.doFinal(ciphertext), Charsets.UTF_8) == "tidalbooks-storage-check")
    }
    override fun isUserLoggedIn(): Boolean = saved()?.optString("access_token")?.isNotBlank() == true
    private fun credentials(session: JSONObject) = Credentials(
        clientId = AlternativeClientProfile.CLIENT_ID, requestedScopes = setOf("r_usr"), clientUniqueKey = null,
        grantedScopes = session.optString("scope", "r_usr").split(Regex("[ ,]+")).filter(String::isNotBlank).toSet(),
        userId = session.optString("user_id").takeIf { it.isNotBlank() }
            ?: session.optJSONObject("user")?.optString("userId")?.takeIf { it.isNotBlank() },
        expires = session.optLong("expires_at"), token = session.getString("access_token"),
    )
    private suspend fun post(endpoint: String, fields: Map<String, String>): Pair<Int, JSONObject> = withContext(Dispatchers.IO) {
        val body = FormBody.Builder().apply { fields.forEach { (key, value) -> add(key, value) } }.build()
        val request = Request.Builder().url("https://auth.tidal.com/v1/oauth2/$endpoint").post(body).build()
        http.newCall(request).execute().use { response ->
            response.code to runCatching { JSONObject(response.body?.string() ?: "{}") }.getOrDefault(JSONObject())
        }
    }
    override suspend fun getCredentials(apiErrorSubStatus: String?): AuthResult<Credentials> = lock.withLock {
        var session = saved() ?: return@withLock AuthResult.Failure(null)
        if (session.optLong("expires_at") <= System.currentTimeMillis()/1000 + 60) {
            val refresh = session.optString("refresh_token")
            if (refresh.isBlank()) return@withLock AuthResult.Failure(null)
            val (status, updated) = post("token", mapOf("client_id" to AlternativeClientProfile.CLIENT_ID,
                "client_secret" to AlternativeClientProfile.CLIENT_SECRET, "grant_type" to "refresh_token", "refresh_token" to refresh))
            if (status != 200 || updated.optString("access_token").isBlank()) return@withLock AuthResult.Failure(null)
            if (updated.optString("refresh_token").isBlank()) updated.put("refresh_token", refresh)
            if (!updated.has("user_id") && session.has("user_id")) updated.put("user_id", session.get("user_id"))
            if (!updated.has("user") && session.has("user")) updated.put("user", session.get("user"))
            updated.put("expires_at", System.currentTimeMillis()/1000 + updated.optLong("expires_in", 3600))
            save(updated); session = updated
        }
        AuthResult.Success(credentials(session))
    }
    data class DeviceLogin(val deviceCode: String, val userCode: String, val verificationUrl: String, val intervalSeconds: Long, val expiresSeconds: Long)
    suspend fun begin(): DeviceLogin {
        withContext(Dispatchers.IO) { verifySecureStorage() }
        val (status, data) = post("device_authorization", mapOf("client_id" to AlternativeClientProfile.CLIENT_ID, "scope" to "r_usr"))
        check(status == 200 && data.optString("deviceCode").isNotBlank()) { "Alternative Geräteanmeldung nicht verfügbar (HTTP $status)." }
        val raw = data.optString("verificationUriComplete").ifBlank { data.getString("verificationUri") }
        val url = if (raw.startsWith("https://")) raw else "https://$raw"
        val parsed = url.toHttpUrl()
        check(parsed.isHttps && parsed.host in setOf("link.tidal.com", "login.tidal.com", "tidal.com")) { "Unerwartete TIDAL-Anmeldeseite." }
        return DeviceLogin(data.getString("deviceCode"), data.getString("userCode"), url,
            data.optLong("interval", 5).coerceIn(2, 30), data.optLong("expiresIn", 300).coerceIn(30, 900))
    }
    suspend fun complete(login: DeviceLogin) {
        val expiresAt = SystemClock.elapsedRealtime() + login.expiresSeconds*1000
        var interval = login.intervalSeconds
        while (SystemClock.elapsedRealtime() < expiresAt) {
            delay(interval*1000)
            val result = try {
                post("token", mapOf("client_id" to AlternativeClientProfile.CLIENT_ID,
                    "client_secret" to AlternativeClientProfile.CLIENT_SECRET, "device_code" to login.deviceCode,
                    "grant_type" to "urn:ietf:params:oauth:grant-type:device_code", "scope" to "r_usr"))
            } catch (_: IOException) {
                // Opening the browser can temporarily freeze or disconnect the background app.
                // Keep the same approval alive instead of discarding the device login.
                interval = (interval+2).coerceAtMost(30)
                continue
            }
            val (status, data) = result
            when (deviceLoginDecision(status, data.optString("access_token").isNotBlank(),
                data.optString("error"), data.optInt("sub_status"))) {
                DeviceLoginDecision.COMPLETE -> {
                    data.put("expires_at", System.currentTimeMillis()/1000 + data.optLong("expires_in", 3600))
                    // Preserve the granted session before any subsequent metadata request.
                    // A retry after a network error must not ask for another account approval.
                    withContext(Dispatchers.IO) { save(data) }
                    activate(data)
                    return
                }
                DeviceLoginDecision.WAIT -> Unit
                DeviceLoginDecision.SLOW_DOWN -> interval = (interval+5).coerceAtMost(30)
                DeviceLoginDecision.FAIL -> {
                    error("Alternative Anmeldung nicht abgeschlossen (HTTP $status). Bitte erneut starten.")
                }
            }
        }
        error("Der Anmeldecode ist abgelaufen. Bitte erneut starten.")
    }
    suspend fun resumePendingSession(): Boolean {
        if (selected) return isUserLoggedIn()
        val data = withContext(Dispatchers.IO) { saved() } ?: return false
        if (data.optLong("expires_at") <= System.currentTimeMillis()/1000+60) return false
        activate(data)
        return true
    }
    private suspend fun activate(data: JSONObject) {
        var confirmed = false
        for (attempt in 0..2) {
            try { confirmUser(data); confirmed = true; break }
            catch (e: IOException) { if (attempt == 2) throw e; delay(2000) }
        }
        check(confirmed) { "Die alternative Benutzersitzung konnte noch nicht bestätigt werden." }
        withContext(Dispatchers.IO) { save(data) }
        selected = true
        bus.emit(CredentialsUpdatedMessage(credentials(data)))
    }
    private suspend fun confirmUser(data: JSONObject) = withContext(Dispatchers.IO) {
        val providedId = data.optString("user_id").takeIf { it.isNotBlank() && it != "null" }
            ?: data.optJSONObject("user")?.optString("userId")?.takeIf { it.isNotBlank() && it != "null" }
        if (providedId != null) {
            data.put("user_id", providedId)
            data.put("country_code", data.optJSONObject("user")?.optString("countryCode").orEmpty())
            return@withContext
        }
        val request = Request.Builder().url("https://api.tidal.com/v1/sessions")
            .header("Authorization", "Bearer ${data.getString("access_token")}").build()
        http.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Alternative Sitzung konnte nicht bestätigt werden (HTTP ${response.code})." }
            val session = JSONObject(response.body?.string() ?: "{}")
            val userId = session.optString("userId")
            check(userId.isNotBlank() && userId != "null") { "TIDAL hat keine Benutzersitzung bestätigt." }
            data.put("user_id", userId)
            data.put("country_code", session.optString("countryCode"))
        }
        Unit
    }
}
