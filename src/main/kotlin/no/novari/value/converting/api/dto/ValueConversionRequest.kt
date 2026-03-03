package no.novari.value.converting.api.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ValueConversionRequest(
    val displayName: String,
    val fromApplicationId: Long,
    val fromTypeId: String,
    val toApplicationId: String,
    val toTypeId: String,
    val convertingMap: Map<String, String>,
)
