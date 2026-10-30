package de.tidalbooks.prototype

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun tidalCatalogUrl(value: String): HttpUrl {
    val url = value.toHttpUrlOrNull()

    require(
        url != null &&
            url.isHttps &&
            url.host == "openapi.tidal.com" &&
            url.port == 443 &&
            url.username.isEmpty() &&
            url.password.isEmpty() &&
            url.fragment == null &&
            url.encodedPath.startsWith("/v2/")
    ) {
        translated(
            "Ungültige TIDAL-Katalogadresse. Der Zugriff wurde zum Schutz der Anmeldung abgebrochen."
        )
    }

    return url
}

fun nextTidalCatalogPage(current: String, href: String): String {
    val base = tidalCatalogUrl(current)
    var next =
        base.resolve(href)
            ?: throw IllegalArgumentException(
                translated("TIDAL hat eine ungültige Folgeseite geliefert.")
            )

    if ("/v2${next.encodedPath}" == base.encodedPath) {
        next = next.newBuilder().encodedPath("/v2${next.encodedPath}").build()
    }

    val validated = tidalCatalogUrl(next.toString())

    require(validated.encodedPath == base.encodedPath) {
        translated(
            "Die TIDAL-Folgeseite verweist auf ein anderes Album. Das Laden wurde abgebrochen."
        )
    }

    return validated.toString()
}
