package no.novari.value.converting.domain

import no.novari.value.converting.api.dto.ValueConversionRequest
import no.novari.value.converting.api.dto.ValueConversionResponse
import no.novari.value.converting.api.dto.ValueConversionSnapshot
import no.novari.value.converting.api.exception.ValueConversionDataIntegrityException
import no.novari.value.converting.api.exception.ValueConversionValidationException
import org.springframework.stereotype.Component

@Component
class ValueConversionMapper {
    fun toEntity(request: ValueConversionRequest): ValueConversion {
        return ValueConversion(
            displayName = request.displayName,
            fromApplicationId = request.fromApplicationId,
            fromTypeId = request.fromTypeId,
            toApplicationId = request.toApplicationId,
            toTypeId = request.toTypeId,
            convertingMap = trimConvertingMap(request),
        )
    }

    fun updateEntity(
        valueConversion: ValueConversion,
        request: ValueConversionRequest,
    ): ValueConversion {
        val trimmedConvertingMap = trimConvertingMap(request)

        valueConversion.displayName = request.displayName
        valueConversion.fromApplicationId = request.fromApplicationId
        valueConversion.fromTypeId = request.fromTypeId
        valueConversion.toApplicationId = request.toApplicationId
        valueConversion.toTypeId = request.toTypeId
        valueConversion.convertingMap
            .keys
            .filterNot(trimmedConvertingMap::containsKey)
            .forEach(valueConversion.convertingMap::remove)
        valueConversion.convertingMap.putAll(trimmedConvertingMap)

        return valueConversion
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

    fun toSnapshot(valueConversion: ValueConversion): ValueConversionSnapshot {
        val valueConversionId = valueConversion.id
        return ValueConversionSnapshot(
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
            convertingMap = valueConversion.convertingMap.toMap(),
        )
    }

    private fun <T : Any> requiredField(
        value: T?,
        fieldName: String,
        valueConversionId: Long?,
    ): T {
        return value ?: throw ValueConversionDataIntegrityException(valueConversionId, fieldName)
    }

    private fun trimConvertingMap(request: ValueConversionRequest): MutableMap<String, String> {
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

        return trimmedEntries.toMap(mutableMapOf())
    }
}
