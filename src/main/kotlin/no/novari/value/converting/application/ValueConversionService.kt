package no.novari.value.converting.application

import no.novari.value.converting.api.dto.ValueConversionRequest
import no.novari.value.converting.api.dto.ValueConversionResponse
import no.novari.value.converting.domain.ValueConversionMapper
import no.novari.value.converting.infrastructure.persistence.ValueConversionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ValueConversionService(
    private val valueConversionMapper: ValueConversionMapper,
    private val valueConversionRepository: ValueConversionRepository,
) {
    fun findDistinctSourceApplicationIds(): Set<Long> = valueConversionRepository.findDistinctSourceApplicationIds()

    fun findAllBySourceApplicationIds(
        pageable: Pageable,
        includeConversionMap: Boolean,
        sourceApplicationIds: Set<Long>,
    ): Page<ValueConversionResponse> {
        return valueConversionRepository
            .findAllByFromApplicationIdIn(pageable, sourceApplicationIds)
            .map { valueConversion ->
                valueConversionMapper.toResponse(valueConversion, includeConversionMap)
            }
    }

    fun findById(valueConversionId: Long): ValueConversionResponse? {
        return valueConversionRepository
            .findById(valueConversionId)
            .map { valueConversion ->
                valueConversionMapper.toResponse(valueConversion, true)
            }.orElse(null)
    }

    fun save(request: ValueConversionRequest): ValueConversionResponse {
        return valueConversionMapper.toResponse(
            valueConversionRepository.save(
                valueConversionMapper.toEntity(request),
            ),
            true,
        )
    }
}
