package de.tidalbooks.prototype

import java.net.URI

/** Auth SDK 0.10.1 needs the entire URI, including its base URL and untouched query. */
fun dispatchLoginRedirect(value: String, finishLogin: (String) -> Unit): Boolean {
    val uri = runCatching { URI(value) }.getOrNull() ?: return false
    if (uri.scheme != "tidalbooks" || uri.rawAuthority != "oauth" || uri.rawPath != "/callback" ||
        uri.rawQuery.isNullOrEmpty() || uri.rawFragment != null) return false
    finishLogin(value)
    return true
}
