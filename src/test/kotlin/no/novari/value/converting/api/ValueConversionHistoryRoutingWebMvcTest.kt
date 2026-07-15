package no.novari.value.converting.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.novari.flyt.audit.history.EntityHistoryEntryDto
import no.novari.flyt.audit.history.HistoryEntryDto
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import no.novari.value.converting.api.dto.ValueConversionResponse
import no.novari.value.converting.application.ValueConversionService
import no.novari.value.converting.domain.ValueConversion
import no.novari.value.converting.domain.ValueConversionHistoryService
import no.novari.value.converting.infrastructure.persistence.ValueConversionRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.core.Authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ValueConversionHistoryRoutingWebMvcTest {
    @Mock
    private lateinit var valueConversionService: ValueConversionService

    @Mock
    private lateinit var valueConversionRepository: ValueConversionRepository

    @Mock
    private lateinit var userAuthorizationService: UserAuthorizationService

    @Mock
    private lateinit var valueConversionHistoryService: ValueConversionHistoryService

    @Mock
    private lateinit var authentication: Authentication

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val valueConversionController = ValueConversionController(valueConversionService, userAuthorizationService)
        val historyController =
            ValueConversionHistoryController(
                valueConversionRepository,
                userAuthorizationService,
                valueConversionHistoryService,
            )
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(valueConversionController, historyController)
                .setCustomArgumentResolvers(PageableHandlerMethodArgumentResolver())
                .setMessageConverters(MappingJackson2HttpMessageConverter(jacksonObjectMapper()))
                .build()
    }

    @Test
    fun `GET history routes to allHistory and not to getValueConversion`() {
        whenever(userAuthorizationService.getUserAuthorizedSourceApplicationIds(any())).thenReturn(setOf(1L))
        whenever(valueConversionHistoryService.findAllHistory(any(), any(), any()))
            .thenReturn(PageImpl(emptyList<EntityHistoryEntryDto<ValueConversionResponse, Long>>()))

        mockMvc
            .perform(get("/api/intern/value-convertings/history").principal(authentication))
            .andExpect(status().isOk)

        verify(valueConversionHistoryService).findAllHistory(any(), any(), any())
        verify(valueConversionService, never()).findById(any())
    }

    @Test
    fun `GET id history routes to history for numeric id`() {
        whenever(valueConversionRepository.findById(5L))
            .thenReturn(Optional.of(ValueConversion(id = 5L, fromApplicationId = 1L)))
        whenever(valueConversionHistoryService.findHistory(eq(5L), any(), any()))
            .thenReturn(PageImpl(emptyList<HistoryEntryDto<ValueConversionResponse>>()))

        mockMvc
            .perform(get("/api/intern/value-convertings/5/history").principal(authentication))
            .andExpect(status().isOk)

        verify(valueConversionHistoryService).findHistory(eq(5L), any(), any())
    }

    @Test
    fun `GET numeric id routes to getValueConversion and not to history`() {
        whenever(valueConversionService.findById(5L)).thenReturn(
            ValueConversionResponse(
                id = 5L,
                displayName = "name",
                fromApplicationId = 1L,
                fromTypeId = "fromType",
                toApplicationId = "toApp",
                toTypeId = "toType",
            ),
        )

        mockMvc
            .perform(get("/api/intern/value-convertings/5").principal(authentication))
            .andExpect(status().isOk)

        verify(valueConversionService).findById(5L)
        verify(valueConversionHistoryService, never()).findHistory(any(), any(), any())
    }
}
