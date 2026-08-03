package no.novari.value.converting.domain

import jakarta.persistence.EntityManager
import no.novari.flyt.audit.actor.ActorDisplayResolver
import no.novari.flyt.audit.history.EnversHistoryService
import no.novari.value.converting.api.dto.ValueConversionSnapshot
import org.springframework.stereotype.Service

@Service
class ValueConversionHistoryService(
    entityManager: EntityManager,
    displayResolver: ActorDisplayResolver,
    private val valueConversionMapper: ValueConversionMapper,
) : EnversHistoryService<ValueConversion, Long, ValueConversionSnapshot>(
        ValueConversion::class.java,
        entityManager,
        displayResolver,
    ) {
    public override fun mapSnapshot(entity: ValueConversion) = valueConversionMapper.toSnapshot(entity)
}
