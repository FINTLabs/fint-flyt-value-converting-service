package no.novari.value.converting.api.dto

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import no.novari.value.converting.api.exception.InvalidRequestParameterException
import no.novari.value.converting.application.ValueConversionFilter
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.Instant
import java.util.UUID

class ValueConversionFilterParams {
    @field:NotNull
    @field:Min(0)
    var page: Int? = null

    @field:NotNull
    @field:Min(1)
    @field:Max(1000)
    var size: Int? = null

    @field:NotBlank
    var sortProperty: String? = null

    @field:NotNull
    var sortDirection: Sort.Direction? = null

    var excludeConvertingMap: Boolean = false
    var sourceApplicationIds: Set<Long> = emptySet()
    var fromTypeId: String? = null
    var toApplicationId: String? = null
    var toTypeId: String? = null
    var displayName: String? = null
    var createdBy: UUID? = null
    var createdAtFrom: Instant? = null
    var createdAtTo: Instant? = null
    var modifiedBy: UUID? = null
    var modifiedAtFrom: Instant? = null
    var modifiedAtTo: Instant? = null

    @get:AssertTrue(message = "createdAtFrom must be before or equal to createdAtTo")
    val isCreatedAtRangeValid: Boolean
        get() = isValidRange(createdAtFrom, createdAtTo)

    @get:AssertTrue(message = "modifiedAtFrom must be before or equal to modifiedAtTo")
    val isModifiedAtRangeValid: Boolean
        get() = isValidRange(modifiedAtFrom, modifiedAtTo)

    fun toPageRequest(): PageRequest =
        PageRequest
            .of(
                checkNotNull(page),
                checkNotNull(size),
            ).withSort(
                checkNotNull(sortDirection),
                checkNotNull(sortProperty).toEntitySortProperty(),
            )

    fun toFilter(): ValueConversionFilter =
        ValueConversionFilter(
            sourceApplicationIds = sourceApplicationIds,
            fromTypeId = fromTypeId,
            toApplicationId = toApplicationId,
            toTypeId = toTypeId,
            displayName = displayName,
            createdBy = createdBy,
            createdAtFrom = createdAtFrom,
            createdAtTo = createdAtTo,
            modifiedBy = modifiedBy,
            modifiedAtFrom = modifiedAtFrom,
            modifiedAtTo = modifiedAtTo,
        ).normalized()

    private fun String.toEntitySortProperty(): String =
        SORT_PROPERTIES[this]
            ?: throw InvalidRequestParameterException(
                "Validation error: 'sortProperty' must be one of ${SORT_PROPERTIES.keys.sorted().joinToString()}",
            )

    private fun isValidRange(
        from: Instant?,
        to: Instant?,
    ): Boolean = from == null || to == null || !from.isAfter(to)

    companion object {
        private val SORT_PROPERTIES =
            mapOf(
                "id" to "id",
                "displayName" to "displayName",
                "sourceApplicationIds" to "fromApplicationId",
                "fromApplicationId" to "fromApplicationId",
                "fromTypeId" to "fromTypeId",
                "toApplicationId" to "toApplicationId",
                "toTypeId" to "toTypeId",
                "createdAt" to "createdAt",
                "createdBy" to "createdBy",
                "modifiedAt" to "lastModifiedAt",
                "modifiedBy" to "lastModifiedBy",
            )
    }
}
