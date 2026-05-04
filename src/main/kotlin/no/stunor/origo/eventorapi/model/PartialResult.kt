package no.stunor.origo.eventorapi.model

/**
 * Represents a response that may contain partial results due to timeout.
 * Used to return HTTP 206 Partial Content when not all requested data could be retrieved.
 */
data class PartialResult<T>(
    val data: T,
    val isPartial: Boolean = false
)

