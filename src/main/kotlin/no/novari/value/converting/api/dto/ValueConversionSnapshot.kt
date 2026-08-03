package no.novari.value.converting.api.dto

/**
 * Tilstanden til en verdikonvertering slik den var i en gitt revisjon, brukt som `snapshot` i
 * historikk-API-et. Bevisst uten audit-sidecar-felt (`createdBy`/`lastModifiedBy` m.fl.): de er
 * `@NotAudited` og dermed alltid tomme i en rekonstruert revisjon, og hvem/når per revisjon
 * eksponeres allerede på historikk-rad-nivå (`actor`/`actorDisplay`/`timestamp`).
 */
data class ValueConversionSnapshot(
    val id: Long?,
    val displayName: String,
    val fromApplicationId: Long,
    val fromTypeId: String,
    val toApplicationId: String,
    val toTypeId: String,
    val convertingMap: Map<String, String>,
)
