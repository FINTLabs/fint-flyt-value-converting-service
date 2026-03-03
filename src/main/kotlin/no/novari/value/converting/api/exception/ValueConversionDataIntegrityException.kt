package no.novari.value.converting.api.exception

class ValueConversionDataIntegrityException(
    valueConversionId: Long?,
    fieldName: String,
) : RuntimeException(
        "Value conversion data integrity violation: field '$fieldName' was null for valueConversionId=$valueConversionId",
    )
