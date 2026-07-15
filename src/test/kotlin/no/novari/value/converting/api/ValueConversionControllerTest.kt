package no.novari.value.converting.api

import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import no.novari.value.converting.api.dto.ValueConversionRequest
import no.novari.value.converting.api.dto.ValueConversionResponse
import no.novari.value.converting.api.exception.ValueConversionNotFoundException
import no.novari.value.converting.application.ValueConversionService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.server.ResponseStatusException

@ExtendWith(MockitoExtension::class)
class ValueConversionControllerTest {
    @Mock
    private lateinit var valueConversionService: ValueConversionService

    @Mock
    private lateinit var userAuthorizationService: UserAuthorizationService

    @Mock
    private lateinit var authentication: Authentication

    private lateinit var pageRequest: PageRequest

    @BeforeEach
    fun setUp() {
        pageRequest = PageRequest.of(0, 10, Sort.Direction.ASC, "property")
    }

    private fun getController(): ValueConversionController {
        return ValueConversionController(
            valueConversionService,
            userAuthorizationService,
        )
    }

    private fun validRequest(fromApplicationId: Long = 1L): ValueConversionRequest {
        return ValueConversionRequest(
            displayName = "Display name",
            fromApplicationId = fromApplicationId,
            fromTypeId = "fromType",
            toApplicationId = "toAppId",
            toTypeId = "toType",
            convertingMap = mapOf("A" to "B"),
        )
    }

    private fun validResponse(
        id: Long? = 1L,
        fromApplicationId: Long = 1L,
    ): ValueConversionResponse {
        return ValueConversionResponse(
            id = id,
            displayName = "Display name",
            fromApplicationId = fromApplicationId,
            fromTypeId = "fromType",
            toApplicationId = "toAppId",
            toTypeId = "toType",
            convertingMap = mapOf("A" to "B"),
        )
    }

    @Test
    @DisplayName("returns value conversions filtered by source application IDs")
    fun `getting value conversions should filter by source application ids`() {
        val mockSourceApplicationIds = setOf(1L, 2L)
        whenever(userAuthorizationService.getUserAuthorizedSourceApplicationIds(authentication))
            .thenReturn(mockSourceApplicationIds)

        val mockContent = listOf(mock<ValueConversionResponse>())
        val mockPage = mock<Page<ValueConversionResponse>>()
        whenever(mockPage.content).thenReturn(mockContent)
        whenever(
            valueConversionService.findAllBySourceApplicationIds(
                pageRequest,
                false,
                mockSourceApplicationIds,
            ),
        ).thenReturn(mockPage)

        val response =
            getController()
                .getValueConversions(
                    authentication = authentication,
                    page = 0,
                    size = 10,
                    sortProperty = "property",
                    sortDirection = Sort.Direction.ASC,
                    excludeConversionMap = true,
                )

        verify(userAuthorizationService).getUserAuthorizedSourceApplicationIds(authentication)
        verify(valueConversionService).findAllBySourceApplicationIds(
            pageable = pageRequest,
            includeConversionMap = false,
            sourceApplicationIds = mockSourceApplicationIds,
        )

        assertThat(response.content).isEqualTo(mockContent)
    }

    @Test
    @DisplayName("returns value conversion if found")
    fun `getting value conversion by id should return conversion when found`() {
        val dto = validResponse(id = 1L)
        whenever(valueConversionService.findById(1L)).thenReturn(dto)

        val response = getController().getValueConversion(authentication, 1L)

        verify(valueConversionService).findById(1L)

        assertThat(response).isEqualTo(dto)
    }

    @Test
    @DisplayName("throws forbidden if user does not have access to value conversion")
    fun `getting value conversion by id should throw forbidden when user has no access`() {
        doThrow(ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
            .whenever(userAuthorizationService)
            .checkIfUserHasAccessToSourceApplication(authentication, 1L)

        whenever(valueConversionService.findById(1L))
            .thenReturn(validResponse(fromApplicationId = 1L))

        assertThatThrownBy { getController().getValueConversion(authentication, 1L) }
            .isInstanceOf(ResponseStatusException::class.java)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN)
            .hasMessageContaining("Forbidden")

        verify(userAuthorizationService).checkIfUserHasAccessToSourceApplication(authentication, 1L)
        verify(valueConversionService).findById(1L)
    }

    @Test
    @DisplayName("throws not found when value conversion is missing")
    fun `getting value conversion by id should throw not found when conversion is missing`() {
        whenever(valueConversionService.findById(1L)).thenReturn(null)

        assertThatThrownBy { getController().getValueConversion(authentication, 1L) }
            .isInstanceOf(ValueConversionNotFoundException::class.java)

        verify(valueConversionService).findById(1L)
    }

    @Test
    @DisplayName("throws forbidden if user does not have access to value conversion on POST")
    fun `posting value conversion should throw forbidden when user has no access`() {
        doThrow(ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
            .whenever(userAuthorizationService)
            .checkIfUserHasAccessToSourceApplication(any(), any())

        val request = validRequest(fromApplicationId = 1L)

        assertThatThrownBy { getController().postValueConversion(authentication, request) }
            .isInstanceOf(ResponseStatusException::class.java)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN)
            .hasMessageContaining("Forbidden")

        verify(userAuthorizationService).checkIfUserHasAccessToSourceApplication(any(), any())
    }

    @Test
    @DisplayName("posts value conversion with no validation errors")
    fun `posting value conversion should return saved conversion when request is valid`() {
        val request = validRequest()
        val responseBody = validResponse()
        whenever(valueConversionService.save(request)).thenReturn(responseBody)

        val response = getController().postValueConversion(authentication, request)

        verify(valueConversionService).save(request)

        assertThat(response).isEqualTo(responseBody)
    }
}
