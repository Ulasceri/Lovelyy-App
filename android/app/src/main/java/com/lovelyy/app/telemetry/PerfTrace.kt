package com.lovelyy.app.telemetry

/**
 * Times [block] as a custom Performance Monitoring trace, e.g. `trace("pairing_join") { ... }`.
 * Works around suspend calls too (it's inline). Does nothing when Firebase isn't configured.
 * Trace names: snake_case, no personal data, at most 100 characters.
 */
inline fun <T> trace(name: String, block: () -> T): T {
    val t = Telemetry.startTrace(name)
    try {
        return block()
    } finally {
        t?.stop()
    }
}
