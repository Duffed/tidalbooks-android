package de.tidalbooks.prototype

enum class DeviceLoginDecision {
    COMPLETE,
    WAIT,
    SLOW_DOWN,
    FAIL,
}

fun deviceLoginDecision(
    status: Int,
    hasToken: Boolean,
    error: String,
    subStatus: Int,
): DeviceLoginDecision =
    when {
        status == 200 && hasToken -> DeviceLoginDecision.COMPLETE
        status == 429 || (status == 400 && error == "slow_down") -> DeviceLoginDecision.SLOW_DOWN
        status in 500..599 -> DeviceLoginDecision.WAIT
        status == 400 && (error == "authorization_pending" || subStatus == 1002) ->
            DeviceLoginDecision.WAIT
        else -> DeviceLoginDecision.FAIL
    }
