package no.novari.flyt.value.converting.api.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Forespørsel for å opprette eller oppdatere en verdikonvertering.")
data class ValueConversionRequest(
    @field:Schema(description = "Visningsnavn for verdikonverteringen.", example = "FS til ERP")
    val displayName: String,
    @field:Schema(description = "ID-en til kildeapplikasjonen.", example = "1001")
    val fromApplicationId: Long,
    @field:Schema(description = "Type-ID i kildeapplikasjonen.", example = "FS.Student")
    val fromTypeId: String,
    @field:Schema(description = "ID-en til målapplikasjonen.", example = "erp")
    val toApplicationId: String,
    @field:Schema(description = "Type-ID i målapplikasjonen.", example = "ERP.Student")
    val toTypeId: String,
    @field:Schema(description = "Oppslag fra kildeverdi til målverdi.")
    val convertingMap: Map<String, String>,
)
