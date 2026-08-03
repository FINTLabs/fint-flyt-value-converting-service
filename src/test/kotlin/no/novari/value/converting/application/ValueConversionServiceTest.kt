package no.novari.value.converting.application

import no.novari.flyt.audit.actor.ActorDisplayResolver
import no.novari.value.converting.api.dto.ValueConversionRequest
import no.novari.value.converting.api.dto.ValueConversionResponse
import no.novari.value.converting.domain.ValueConversion
import no.novari.value.converting.domain.ValueConversionMapper
import no.novari.value.converting.infrastructure.persistence.ValueConversionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ValueConversionServiceTest {
    @Mock
    private lateinit var valueConversionMapper: ValueConversionMapper

    @Mock
    private lateinit var valueConversionRepository: ValueConversionRepository

    @Mock
    private lateinit var actorDisplayResolver: ActorDisplayResolver

    @InjectMocks
    private lateinit var service: ValueConversionService

    @Test
    fun `finding all by source application ids should return mapped page`() {
        val pageable = PageRequest.of(0, 10)
        val includeConversionMap = false
        val sourceApplicationIds = setOf(1L, 2L)

        val valueConversion =
            ValueConversion(
                id = 1L,
                displayName = "displayName",
                fromApplicationId = 2L,
                fromTypeId = "fromTypeId",
                toApplicationId = "toApplicationId",
                toTypeId = "toTypeId",
                convertingMap = hashMapOf(),
            )
        val valueConversionPage = PageImpl(listOf(valueConversion), pageable, 1)

        val response =
            ValueConversionResponse(
                id = 1L,
                displayName = "displayName",
                fromApplicationId = 2L,
                fromTypeId = "fromTypeId",
                toApplicationId = "toApplicationId",
                toTypeId = "toTypeId",
                convertingMap = null,
            )

        whenever(valueConversionRepository.findAllByFromApplicationIdIn(pageable, sourceApplicationIds))
            .thenReturn(valueConversionPage)
        whenever(actorDisplayResolver.resolveAll(any())).thenReturn(emptyMap())
        whenever(
            valueConversionMapper.toResponse(eq(valueConversion), eq(includeConversionMap), anyOrNull(), anyOrNull()),
        ).thenReturn(response)

        val actualPage = service.findAllBySourceApplicationIds(pageable, includeConversionMap, sourceApplicationIds)

        assertEquals(1, actualPage.totalElements)
        assertEquals(response, actualPage.content[0])
    }

    @Test
    fun `finding by existing id should return response`() {
        val valueConversionId = 1L
        val expectedResponse = mock<ValueConversionResponse>()
        whenever(valueConversionRepository.findById(valueConversionId)).thenReturn(Optional.of(mock<ValueConversion>()))
        whenever(actorDisplayResolver.resolve(anyOrNull())).thenReturn(null)
        whenever(valueConversionMapper.toResponse(any(), any(), anyOrNull(), anyOrNull())).thenReturn(expectedResponse)

        val result = service.findById(valueConversionId)

        verify(valueConversionRepository).findById(valueConversionId)
        verify(valueConversionMapper).toResponse(any(), eq(true), anyOrNull(), anyOrNull())
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `finding by non existing id should return null`() {
        val valueConversionId = 1L
        whenever(valueConversionRepository.findById(valueConversionId)).thenReturn(Optional.empty())

        val result = service.findById(valueConversionId)

        verify(valueConversionRepository).findById(valueConversionId)
        assertNull(result)
    }

    @Test
    fun `saving value conversion should return saved response`() {
        val request = mock<ValueConversionRequest>()
        val entity = mock<ValueConversion>()
        val savedValueConversion = mock<ValueConversion>()
        val expectedResponse = mock<ValueConversionResponse>()

        whenever(valueConversionMapper.toEntity(request)).thenReturn(entity)
        whenever(valueConversionRepository.save(any<ValueConversion>())).thenReturn(savedValueConversion)
        whenever(actorDisplayResolver.resolve(anyOrNull())).thenReturn(null)
        whenever(valueConversionMapper.toResponse(any(), any(), anyOrNull(), anyOrNull())).thenReturn(expectedResponse)

        val result = service.save(request)

        verify(valueConversionMapper).toEntity(request)
        verify(valueConversionRepository).save(any<ValueConversion>())
        verify(valueConversionMapper).toResponse(any(), eq(true), anyOrNull(), anyOrNull())
        assertEquals(expectedResponse, result)
    }
}
