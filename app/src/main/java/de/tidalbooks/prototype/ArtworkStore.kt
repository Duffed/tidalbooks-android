package de.tidalbooks.prototype

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

internal class ArtworkStore(
    private val directory: File,
    private val diskLimit: Long = 64L * 1024 * 1024,
    private val fetch: (String) -> ByteArray? = ::downloadArtwork,
) {
    private val memory =
        object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
        }
    // Bounded locks coalesce concurrent requests without retaining a lock per library item.
    private val locks = Array(32) { Mutex() }
    private val diskLock = Any()

    fun peek(url: String): Bitmap? = memory.get(url)

    suspend fun loadCached(url: String): Bitmap? =
        withContext(Dispatchers.IO) {
            if (!validArtworkUrl(url)) return@withContext null

            memory.get(url)?.let {
                return@withContext it
            }
            val file = cacheFile(url)
            val bitmap =
                synchronized(diskLock) {
                    runCatching {
                        if (file.length() in 1..MAX_ARTWORK_BYTES.toLong()) decode(file.readBytes())
                        else null
                    }
                        .getOrNull()
                        ?.also { file.setLastModified(System.currentTimeMillis()) }
                }

            bitmap?.also { memory.put(url, it) }
        }

    private fun cacheFile(url: String): File =
        File(
            directory,
            MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString("") {
                "%02x".format(it)
            } + ".cover",
        )

    suspend fun load(url: String): Bitmap? =
        withContext(Dispatchers.IO) {
            if (!validArtworkUrl(url)) return@withContext null

            locks[(url.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
                memory.get(url)?.let {
                    return@withLock it
                }
                loadCached(url)?.let {
                    return@withLock it
                }
                val file = cacheFile(url)
                val name = file.name
                val bytes = runCatching { fetch(url) }.getOrNull() ?: return@withLock null
                val bitmap = runCatching { decode(bytes) }.getOrNull() ?: return@withLock null

                memory.put(url, bitmap)
                synchronized(diskLock) {
                    // Cache I/O failure must never prevent displaying an already downloaded cover.
                    runCatching {
                        directory.mkdirs()
                        val temp = File(directory, "$name.tmp")

                        try {
                            temp.writeBytes(bytes)
                            if (temp.renameTo(file))
                                file.setLastModified(System.currentTimeMillis())
                        } finally {
                            temp.delete()
                        }

                        val files = directory.listFiles { f -> f.extension == "cover" }.orEmpty()
                        var size = files.sumOf { it.length() }

                        for (old in
                            files.sortedWith(
                                compareBy<File> { it == file }.thenBy { it.lastModified() }
                            )) {
                            if (size <= diskLimit) break

                            val length = old.length()

                            if (old.delete()) size -= length
                        }
                    }
                }
                bitmap
            }
        }

    private fun decode(bytes: ByteArray): Bitmap? {
        if (bytes.size !in 1..MAX_ARTWORK_BYTES) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }

        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth !in 1..2048 || bounds.outHeight !in 1..2048) return null

        val options = BitmapFactory.Options()

        while (
            maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize.coerceAtLeast(1) > 640
        ) {
            options.inSampleSize = options.inSampleSize.coerceAtLeast(1) * 2
        }

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    companion object {
        @Volatile private var instance: ArtworkStore? = null

        fun get(context: Context): ArtworkStore =
            instance
                ?: synchronized(this) {
                    instance
                        ?: ArtworkStore(
                                File(context.applicationContext.cacheDir, "book-artwork-v1")
                            )
                            .also { instance = it }
                }
    }
}

private const val MAX_ARTWORK_BYTES = 4 * 1024 * 1024
private val artworkHttp = OkHttpClient.Builder().followRedirects(false).build()

private fun validArtworkUrl(url: String): Boolean =
    url.toHttpUrlOrNull()?.let {
        it.isHttps && it.host == "resources.tidal.com" && it.encodedPath.startsWith("/images/")
    } == true

private fun downloadArtwork(url: String): ByteArray? =
    artworkHttp.newCall(Request.Builder().url(url).build()).execute().use { response ->
        if (!response.isSuccessful) null
        else
            response.body?.byteStream()?.use stream@{ input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)

                while (true) {
                    val count = input.read(buffer)

                    if (count < 0) break
                    if (output.size() + count > MAX_ARTWORK_BYTES) return@stream null

                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
    }
