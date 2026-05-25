package no.stunor.origo.eventorapi.exception


class EventorConnectionException : RuntimeException(
    "We are currently not able to connect to Eventor. Please try again later."
)
