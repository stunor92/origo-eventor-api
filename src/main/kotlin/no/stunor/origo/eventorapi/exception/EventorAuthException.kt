package no.stunor.origo.eventorapi.exception


class EventorAuthException : RuntimeException(
    "Eventor authentication failed. Please check your credentials and try again."
)
