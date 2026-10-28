package de.tidalbooks.prototype

import android.app.Application
import android.net.Uri
import com.tidal.sdk.auth.TidalAuth
import com.tidal.sdk.auth.model.AuthConfig
import com.tidal.sdk.auth.network.NetworkLogLevel
import com.tidal.sdk.eventproducer.EventProducer
import com.tidal.sdk.eventproducer.model.EventsConfig
import com.tidal.sdk.player.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.time.Duration
import java.util.Base64

interface CatalogGateway {
    fun loginUri(): Uri
    suspend fun finishLogin(redirectUri: String): Boolean
    suspend fun isLoggedIn(): Boolean
    suspend fun album(id: String, country: String): Album
}

class OfficialTidalGateway(private val app: Application, private val scope: CoroutineScope) : CatalogGateway {
    companion object { const val REDIRECT_URI = "tidalbooks://oauth/callback" }
    private val auth = TidalAuth.getInstance(AuthConfig(
        clientId = BuildConfig.TIDAL_CLIENT_ID,
        credentialsKey = "de.tidalbooks.prototype.auth",
        scopes = BuildConfig.TIDAL_SCOPES.split(Regex("\\s+")).filter(String::isNotBlank).toSet(),
        enableCertificatePinning = true,
        logLevel = NetworkLogLevel.NONE,
    ), app)
    private val alternative = AlternativeAuth(app)
    private val credentials: com.tidal.sdk.auth.CredentialsProvider = RoutingCredentialsProvider(auth.credentialsProvider, alternative)
    val alternativeSelected get() = alternative.selected
    private val http = OkHttpClient.Builder().followRedirects(false).build()
    override fun loginUri(): Uri {
        alternative.selected = false
        return auth.auth.initializeLogin(REDIRECT_URI, null)
    }
    override suspend fun finishLogin(redirectUri: String) = auth.auth.finalizeLogin(redirectUri).isSuccess
    override suspend fun isLoggedIn() = credentials.isUserLoggedIn()
    suspend fun beginAlternativeLogin() = alternative.begin()
    suspend fun completeAlternativeLogin(login: AlternativeAuth.DeviceLogin) = alternative.complete(login)
    suspend fun resumeAlternativeLogin() = alternative.resumePendingSession()
    suspend fun probeLegacyPlayback(trackId: String): String = withContext(Dispatchers.IO) {
        require(trackId.matches(Regex("[0-9]+"))) { "Ungültige Track-ID." }
        val token = credentials.getCredentials().successData?.token
            ?: error("Bitte erneut bei TIDAL anmelden.")
        val reports = mutableListOf<String>()
        for (endpoint in listOf("playbackinfopostpaywall", "playbackinfo")) {
            val url = "https://api.tidal.com/v1/tracks/$trackId/$endpoint?audioquality=HIGH&playbackmode=STREAM&assetpresentation=FULL"
            val request = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    reports += "$endpoint: HTTP ${response.code}"
                } else {
                    val data = JSONObject(response.body?.string() ?: "{}")
                    val presentation = data.optString("assetPresentation", "UNBEKANNT")
                    val mime = data.optString("manifestMimeType", "UNBEKANNT")
                    var protection = "nicht bestimmt"
                    val encoded = data.optString("manifest")
                    if (encoded.isNotBlank() && encoded.length < 1_000_000) {
                        val decoded = runCatching { String(Base64.getDecoder().decode(encoded), Charsets.UTF_8) }.getOrNull()
                        if (decoded != null) {
                            protection = if (mime.contains("bts")) {
                                runCatching { JSONObject(decoded).optString("encryptionType", "UNBEKANNT") }.getOrDefault("UNBEKANNT")
                            } else if (mime.contains("dash")) {
                                if (decoded.contains("ContentProtection")) "DRM" else "kein ContentProtection-Eintrag"
                            } else "nicht bestimmt"
                        }
                    }
                    // Deliberately do not expose manifests, stream URLs, tokens, or response bodies.
                    reports += "$endpoint: HTTP 200 · $presentation · $mime · Schutz: $protection"
                }
            }
        }
        reports.joinToString("\n")
    }
    fun createPlayer(): Player {
        val sender = EventProducer.getInstance(credentials,
            EventsConfig(1_000_000, emptySet(), BuildConfig.VERSION_NAME), app, scope).eventSender
        return Player(app, credentials, sender, isDebuggable = false,
            version = BuildConfig.VERSION_NAME)
    }
    private suspend fun json(url: String): JSONObject = withContext(Dispatchers.IO) {
        val parsed = tidalCatalogUrl(url)
        val token = credentials.getCredentials().successData?.token
            ?: error("Anmeldung abgelaufen. Bitte erneut bei TIDAL anmelden.")
        val request = Request.Builder().url(parsed).header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.api+json").build()
        http.newCall(request).execute().use {
            if (!it.isSuccessful) throw CatalogFailure(it.code)
            JSONObject(it.body?.string() ?: error("Leere TIDAL-Antwort."))
        }
    }
    override suspend fun album(id: String, country: String): Album {
        require(id.matches(Regex("[0-9]+"))) { "Die TIDAL-Album-ID muss aus Ziffern bestehen." }
        require(country.matches(Regex("[A-Z]{2}"))) { "Ländercode muss aus zwei Buchstaben bestehen." }
        if (alternative.selected) return legacyAlbum(id, country)
        val root = "https://openapi.tidal.com/v2"
        val metadata = json("$root/albums/$id?countryCode=$country").getJSONObject("data").getJSONObject("attributes")
        val tracks = mutableListOf<Track>()
        var next: String? = "$root/albums/$id/relationships/items?countryCode=$country&include=items"
        val visited = mutableSetOf<String>()
        while (next != null) {
            check(visited.add(next)) { "TIDAL liefert eine wiederholte Albumseite." }
            val page = json(next)
            val included = page.optJSONArray("included")
            val resources = (0 until (included?.length() ?: 0)).map { included!!.getJSONObject(it) }
                .associateBy { it.getString("type") + ":" + it.getString("id") }
            val data = page.getJSONArray("data")
            for (i in 0 until data.length()) {
                val link = data.getJSONObject(i)
                check(link.getString("type") == "tracks") { "Album enthält einen Videotrack; es wird kein Teil übersprungen." }
                val trackId = link.getString("id")
                val resource = resources["tracks:$trackId"] ?: json("$root/tracks/$trackId?countryCode=$country").getJSONObject("data")
                val attrs = resource.getJSONObject("attributes")
                val order = link.getJSONObject("meta")
                tracks += Track(trackId, attrs.getString("title"), Duration.parse(attrs.getString("duration")).toMillis(),
                    order.getInt("volumeNumber"), order.getInt("trackNumber"))
            }
            val rawNext = page.optJSONObject("links")?.opt("next")
            val href = when(rawNext) { is String -> rawNext; is JSONObject -> rawNext.optString("href"); else -> "" }
            next = href.takeIf { it.isNotBlank() }?.let { nextTidalCatalogPage(next!!, it) }
        }
        check(tracks.isNotEmpty()) { "Das Album enthält keine Tracks." }
        check(tracks.size == metadata.getInt("numberOfItems")) { "Die Trackliste ist unvollständig. Bitte erneut laden." }
        return Album(id, metadata.getString("title"), tracks.sortedWith(compareBy(Track::disc, Track::number)))
    }
    private suspend fun legacyJson(path: String): JSONObject = withContext(Dispatchers.IO) {
        val token = credentials.getCredentials().successData?.token ?: error("Bitte erneut anmelden.")
        require(path.startsWith("albums/") && !path.contains(".."))
        val request = Request.Builder().url("https://api.tidal.com/v1/$path")
            .header("Authorization", "Bearer $token").build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw CatalogFailure(response.code)
            JSONObject(response.body?.string() ?: "{}")
        }
    }
    private suspend fun legacyAlbum(id: String, country: String): Album {
        val metadata = legacyJson("albums/$id?countryCode=$country")
        check(metadata.optInt("numberOfVideos") == 0) { "Album enthält Videos; es werden keine Teile übersprungen." }
        val tracks = mutableListOf<Track>()
        var offset = 0
        do {
            val page = legacyJson("albums/$id/tracks?limit=100&offset=$offset&countryCode=$country")
            val items = page.getJSONArray("items")
            check(items.length() > 0 || offset == page.getInt("totalNumberOfItems")) { "Unvollständige TIDAL-Trackliste." }
            for (i in 0 until items.length()) {
                val track = items.getJSONObject(i)
                tracks += Track(track.get("id").toString(), track.getString("title"), track.getLong("duration")*1000,
                    track.getInt("volumeNumber"), track.getInt("trackNumber"))
            }
            offset += items.length()
        } while (offset < page.getInt("totalNumberOfItems"))
        check(tracks.isNotEmpty() && tracks.size == metadata.getInt("numberOfTracks")) { "Unvollständige TIDAL-Trackliste." }
        val author = metadata.optJSONArray("artists")?.let { artists ->
            (0 until artists.length()).map { artists.getJSONObject(it).optString("name") }.filter(String::isNotBlank).distinct().joinToString(", ")
        }.orEmpty().ifBlank { metadata.optJSONObject("artist")?.optString("name").orEmpty() }
        val cover = metadata.optString("cover").takeIf { it.matches(Regex("[a-fA-F0-9-]{36}")) }
            ?.let { "https://resources.tidal.com/images/${it.replace('-', '/')}/640x640.jpg" }
        return Album(id, metadata.getString("title"), tracks.sortedWith(compareBy(Track::disc, Track::number)),author,cover)
    }
}
