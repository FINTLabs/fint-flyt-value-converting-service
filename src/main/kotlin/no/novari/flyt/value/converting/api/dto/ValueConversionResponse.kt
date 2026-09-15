package no.novari.flyt.value.converting.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import no.novari.flyt.audit.actor.Actor
import java.time.Instant

@Schema(description = "En lagret verdikonvertering.")
data class ValueConversionResponse(
    val id: Long? = null,
    val displayName: String,
    val fromApplicationId: Long,
    val fromTypeId: String,
    val toApplicationId: String,
    val toTypeId: String,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val convertingMap: Map<String, String>? = null,
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val createdAt: Instant? = null,
    val createdBy: String? = null,
    val createdByActor: Actor? = null,
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    val lastModifiedAt: Instant? = null,
    val lastModifiedBy: String? = null,
    val lastModifiedByActor: Actor? = null,
)
