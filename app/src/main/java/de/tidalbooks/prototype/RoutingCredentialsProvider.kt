package de.tidalbooks.prototype

import com.tidal.sdk.auth.CredentialsProvider
import kotlinx.coroutines.flow.merge

class RoutingCredentialsProvider(private val official: CredentialsProvider, private val alternative: AlternativeAuth) : CredentialsProvider {
    override val bus = merge(official.bus, alternative.bus)
    override fun isUserLoggedIn() = if (alternative.selected) alternative.isUserLoggedIn() else official.isUserLoggedIn()
    override suspend fun getCredentials(apiErrorSubStatus: String?) =
        if (alternative.selected) alternative.getCredentials(apiErrorSubStatus) else official.getCredentials(apiErrorSubStatus)
}
