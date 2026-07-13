package no.novari.value.converting.application

import no.novari.flyt.audit.actor.ActorDisplayResolver
import no.novari.value.converting.api.dto.ValueConversionRequest
import no.novari.value.converting.api.dto.ValueConversionResponse
import no.novari.value.converting.domain.ValueConversion
import no.novari.value.converting.domain.ValueConversionMapper
import no.novari.value.converting.infrastructure.persistence.ValueConversionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ValueConversionService(
    private val valueConversionMapper: ValueConversionMapper,
    private val valueConversionRepository: ValueConversionRepository,
    private val actorDisplayResolver: ActorDisplayResolver,
) {
    fun findAllBySourceApplicationIds(
        pageable: Pageable,
        includeConversionMap: Boolean,
        sourceApplicationIds: Set<Long>,
    ): Page<ValueConversionResponse> {
        val page = valueConversionRepository.findAllByFromApplicationIdIn(pageable, sourceApplicationIds)
        val createdByDisplays = actorDisplayResolver.resolveAll(page.content.map { it.createdBy })
        val lastModifiedByDisplays = actorDisplayResolver.resolveAll(page.content.map { it.lastModifiedBy })

        return page.map { valueConversion ->
            valueConversionMapper.toResponse(
                valueConversion = valueConversion,
                includeConversionMap = includeConversionMap,
                createdByDisplay = createdByDisplays[valueConversion.createdBy],
                lastModifiedByDisplay = lastModifiedByDisplays[valueConversion.lastModifiedBy],
            )
        }
    }

    fun findById(valueConversionId: Long): ValueConversionResponse? {
        return valueConversionRepository
            .findById(valueConversionId)
            .map { valueConversion -> toHydratedResponse(valueConversion) }
            .orElse(null)
    }

    fun save(request: ValueConversionRequest): ValueConversionResponse {
        val saved = valueConversionRepository.save(valueConversionMapper.toEntity(request))
        return toHydratedResponse(saved)
    }

    private fun toHydratedResponse(valueConversion: ValueConversion) =
        valueConversionMapper.toResponse(
            valueConversion = valueConversion,
            includeConversionMap = true,
            createdByDisplay = actorDisplayResolver.resolve(valueConversion.createdBy),
            lastModifiedByDisplay = actorDisplayResolver.resolve(valueConversion.lastModifiedBy),
        )
}
