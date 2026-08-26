package no.novari.flyt.value.converting.domain

import jakarta.persistence.EntityManager
import no.novari.flyt.audit.actor.ActorDisplayResolver
import no.novari.flyt.value.converting.api.dto.ValueConversionSnapshot
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
    fun `mapSnapshot delegates to ValueConversionMapper toSnapshot`() {
        val service = ValueConversionHistoryService(entityManager, displayResolver, valueConversionMapper)
        val entity = ValueConversion(id = 1L, displayName = "name")
        val expectedSnapshot = mock<ValueConversionSnapshot>()
        whenever(valueConversionMapper.toSnapshot(entity)).thenReturn(expectedSnapshot)

        val snapshot = service.mapSnapshot(entity)

        verify(valueConversionMapper).toSnapshot(entity)
        assertThat(snapshot).isEqualTo(expectedSnapshot)
    }
}
