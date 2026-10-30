package de.tidalbooks.prototype

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class CatalogUrlsTest {
    private val first =
        "https://openapi.tidal.com/v2/albums/270256764/relationships/items?countryCode=DE&include=items"

    @Test
    fun absoluteNextLinkWithoutApiVersionUsesTheV2AlbumEndpoint() {
        // Destination shape observed on the connected Pixel; cursor is synthetic.
        val next =
            nextTidalCatalogPage(
                first,
                "https://openapi.tidal.com/albums/270256764/relationships/items?page%5Bcursor%5D=test-cursor&include=items",
            )
        val parsed = next.toHttpUrl()

        assertEquals("/v2/albums/270256764/relationships/items", parsed.encodedPath)
        assertEquals("test-cursor", parsed.queryParameter("page[cursor]"))
        assertEquals("items", parsed.queryParameter("include"))
        assertEquals("openapi.tidal.com", parsed.host)
    }

    @Test
    fun rootRelativeNextLinkWithoutApiVersionAlsoWorks() {
        val next =
            nextTidalCatalogPage(
                first,
                "/albums/270256764/relationships/items?page%5Bcursor%5D=page2",
            )

        assertEquals("/v2/albums/270256764/relationships/items", next.toHttpUrl().encodedPath)
        assertEquals("page2", next.toHttpUrl().queryParameter("page[cursor]"))
    }

    @Test
    fun versionedAndQueryOnlyNextLinksPreserveEndpointAndCursor() {
        val hrefs =
            listOf(
                "https://openapi.tidal.com/v2/albums/270256764/relationships/items?page%5Bcursor%5D=a%2Bb%3D",
                "/v2/albums/270256764/relationships/items?page%5Bcursor%5D=a%2Bb%3D",
                "?page%5Bcursor%5D=a%2Bb%3D",
            )

        for (href in hrefs) {
            val next = nextTidalCatalogPage(first, href).toHttpUrl()

            assertEquals(first.toHttpUrl().encodedPath, next.encodedPath)
            assertEquals("a+b=", next.queryParameter("page[cursor]"))
        }
    }

    @Test
    fun everyFollowingPageKeepsTheApiVersionExactlyOnce() {
        val second =
            nextTidalCatalogPage(
                first,
                "/albums/270256764/relationships/items?page%5Bcursor%5D=page2",
            )
        val third =
            nextTidalCatalogPage(
                second,
                "/v2/albums/270256764/relationships/items?page%5Bcursor%5D=page3",
            )

        assertEquals("/v2/albums/270256764/relationships/items", third.toHttpUrl().encodedPath)
        assertEquals("page3", third.toHttpUrl().queryParameter("page[cursor]"))
    }

    @Test
    fun paginationCannotRedirectCredentialsToAnotherOrigin() {
        for (href in
            listOf(
                "https://example.com/albums/270256764/relationships/items?cursor=test",
                "http://openapi.tidal.com/albums/270256764/relationships/items?cursor=test",
                "https://openapi.tidal.com:444/albums/270256764/relationships/items?cursor=test",
                "https://user@openapi.tidal.com/albums/270256764/relationships/items?cursor=test",
            )) assertThrows(IllegalArgumentException::class.java) {
            nextTidalCatalogPage(first, href)
        }
    }

    @Test
    fun paginationCannotChangeAlbumOrResource() {
        for (href in
            listOf(
                "/albums/999/relationships/items?cursor=test",
                "/v2/tracks/123",
                "/v2/albums/270256764/relationships/items#fragment",
            )) {
            assertThrows(IllegalArgumentException::class.java) { nextTidalCatalogPage(first, href) }
        }
    }
}
