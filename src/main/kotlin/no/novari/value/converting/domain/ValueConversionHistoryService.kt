package no.novari.value.converting.domain

import jakarta.persistence.EntityManager
import no.novari.flyt.audit.actor.ActorDisplayResolver
import no.novari.flyt.audit.history.EnversHistoryService
import no.novari.value.converting.api.dto.ValueConversionResponse
import org.springframework.stereotype.Service

@Service
class ValueConversionHistoryService(
    entityManager: EntityManager,
    displayResolver: ActorDisplayResolver,
    private val valueConversionMapper: ValueConversionMapper,
) : EnversHistoryService<ValueConversion, Long, ValueConversionResponse>(
        ValueConversion::class.java,
        entityManager,
        displayResolver,
    ) {
    public override fun mapSnapshot(entity: ValueConversion) =
        valueConversionMapper.toResponse(entity, includeConversionMap = true)
}
