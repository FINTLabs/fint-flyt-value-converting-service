package no.novari.value.converting.api.exception

class ValueConversionNotFoundException(
    valueConversionId: Long,
) : RuntimeException("Value conversion with id=$valueConversionId was not found")
