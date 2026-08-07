package no.novari.value.converting.application

import java.time.Instant
import java.util.UUID

data class ValueConversionFilter(
    val sourceApplicationIds: Set<Long> = emptySet(),
    val fromTypeId: String? = null,
    val toApplicationId: String? = null,
    val toTypeId: String? = null,
    val displayName: String? = null,
    val createdBy: UUID? = null,
    val createdAtFrom: Instant? = null,
    val createdAtTo: Instant? = null,
    val modifiedBy: UUID? = null,
    val modifiedAtFrom: Instant? = null,
    val modifiedAtTo: Instant? = null,
) {
    fun normalized(): ValueConversionFilter =
        copy(
            fromTypeId = fromTypeId.trimToNull(),
            toApplicationId = toApplicationId.trimToNull(),
            toTypeId = toTypeId.trimToNull(),
            displayName = displayName.trimToNull(),
        )

    private fun String?.trimToNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)
}
