package no.novari.value.converting.domain

import no.novari.value.converting.api.dto.ValueConversionRequest
import no.novari.value.converting.api.dto.ValueConversionResponse
import no.novari.value.converting.api.exception.ValueConversionDataIntegrityException
import no.novari.value.converting.api.exception.ValueConversionValidationException
import org.springframework.stereotype.Component

@Component
class ValueConversionMapper {
    fun toEntity(request: ValueConversionRequest): ValueConversion {
        val trimmedEntries =
            request.convertingMap
                .entries
                .map { (key, value) -> key.trim() to value.trim() }

        val uniqueTrimmedKeys = trimmedEntries.map { (key, _) -> key }.toSet()
        if (uniqueTrimmedKeys.size != trimmedEntries.size) {
            throw ValueConversionValidationException(
                "Validation error: convertingMap contains duplicate keys after trimming",
            )
        }

        val trimmedMap = trimmedEntries.toMap(mutableMapOf())

        return ValueConversion(
            displayName = request.displayName,
            fromApplicationId = request.fromApplicationId,
            fromTypeId = request.fromTypeId,
            toApplicationId = request.toApplicationId,
            toTypeId = request.toTypeId,
            convertingMap = trimmedMap,
        )
    }

    fun toResponse(
        valueConversion: ValueConversion,
        includeConversionMap: Boolean,
        createdByDisplay: String? = null,
        lastModifiedByDisplay: String? = null,
    ): ValueConversionResponse {
        val valueConversionId = valueConversion.id
        return ValueConversionResponse(
            id = valueConversionId,
            displayName = requiredField(valueConversion.displayName, "displayName", valueConversionId),
            fromApplicationId =
                requiredField(
                    valueConversion.fromApplicationId,
                    "fromApplicationId",
                    valueConversionId,
                ),
            fromTypeId = requiredField(valueConversion.fromTypeId, "fromTypeId", valueConversionId),
            toApplicationId = requiredField(valueConversion.toApplicationId, "toApplicationId", valueConversionId),
            toTypeId = requiredField(valueConversion.toTypeId, "toTypeId", valueConversionId),
            convertingMap = if (includeConversionMap) valueConversion.convertingMap.toMap() else null,
            createdAt = valueConversion.createdAt,
            createdBy = createdByDisplay,
            createdByActor = valueConversion.createdBy,
            lastModifiedAt = valueConversion.lastModifiedAt,
            lastModifiedBy = lastModifiedByDisplay,
            lastModifiedByActor = valueConversion.lastModifiedBy,
        )
    }

    private fun <T : Any> requiredField(
        value: T?,
        fieldName: String,
        valueConversionId: Long?,
    ): T {
        return value ?: throw ValueConversionDataIntegrityException(valueConversionId, fieldName)
    }
}
