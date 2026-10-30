package de.tidalbooks.prototype

/** Android may reclaim a paused/background decoder; this is not a catalog/network failure. */
internal class DecoderRecovery {
    enum class Action { INVALIDATE_PAUSED, RETRY, REPORT }
    private var retried = false

    fun newRequest() { retried = false }

    fun onError(code: String, playbackRequested: Boolean): Action {
        // TIDAL wraps Media3 ERROR_CODE_DECODING_RESOURCES_RECLAIMED as -1:3:4006.
        if (code != "-1:3:4006") return Action.REPORT
        if (!playbackRequested) return Action.INVALIDATE_PAUSED
        if (retried) return Action.REPORT
        retried = true
        return Action.RETRY
    }
}
