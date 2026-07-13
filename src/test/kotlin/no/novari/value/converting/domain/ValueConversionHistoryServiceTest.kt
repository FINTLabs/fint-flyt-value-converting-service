package no.novari.value.converting.domain

import jakarta.persistence.EntityManager
import no.novari.flyt.audit.actor.ActorDisplayResolver
import no.novari.value.converting.api.dto.ValueConversionResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ValueConversionHistoryServiceTest {
    @Mock
    private lateinit var entityManager: EntityManager

    @Mock
    private lateinit var displayResolver: ActorDisplayResolver

    @Mock
    private lateinit var valueConversionMapper: ValueConversionMapper

    @Test
    fun `mapSnapshot delegates to ValueConversionMapper with conversion map included`() {
        val service = ValueConversionHistoryService(entityManager, displayResolver, valueConversionMapper)
        val entity = ValueConversion(id = 1L, displayName = "name")
        val expectedResponse = mock<ValueConversionResponse>()
        whenever(valueConversionMapper.toResponse(entity, true)).thenReturn(expectedResponse)

        val snapshot = service.mapSnapshot(entity)

        verify(valueConversionMapper).toResponse(entity, true)
        assertThat(snapshot).isEqualTo(expectedResponse)
    }
}
