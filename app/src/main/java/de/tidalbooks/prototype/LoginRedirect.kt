package de.tidalbooks.prototype

import java.net.URI

fun dispatchLoginRedirect(value: String, finishLogin: (String) -> Unit): Boolean {
    val uri = runCatching { URI(value) }.getOrNull() ?: return false

    if (
        uri.scheme != "tidalbooks" ||
            uri.rawAuthority != "oauth" ||
            uri.rawPath != "/callback" ||
            uri.rawQuery.isNullOrEmpty() ||
            uri.rawFragment != null
    )
        return false

    finishLogin(value)

    return true
}
