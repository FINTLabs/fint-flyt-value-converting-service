package no.novari.value.converting.api.dto

import com.fasterxml.jackson.annotation.JsonInclude

data class ValueConversionResponse(
    val id: Long? = null,
    val displayName: String,
    val fromApplicationId: Long,
    val fromTypeId: String,
    val toApplicationId: String,
    val toTypeId: String,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val convertingMap: Map<String, String>? = null,
)
