package no.novari.value.converting.api

import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import no.novari.value.converting.api.dto.ValueConversionFilterParams
import no.novari.value.converting.api.dto.ValueConversionRequest
import no.novari.value.converting.api.dto.ValueConversionResponse
import no.novari.value.converting.api.exception.ValueConversionNotFoundException
import no.novari.value.converting.application.ValueConversionFilter
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

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
        pageRequest = PageRequest.of(0, 10, Sort.Direction.ASC, "id")
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

    private fun validFilterParams(): ValueConversionFilterParams =
        ValueConversionFilterParams().apply {
            page = 0
            size = 10
            sortProperty = "id"
            sortDirection = Sort.Direction.ASC
        }

    @Test
    @DisplayName("returns value conversions filtered by source application IDs")
    fun `getting value conversions should filter by source application ids`() {
        val candidateSourceApplicationIds = setOf(1L, 2L, 3L)
        val mockSourceApplicationIds = setOf(1L, 2L)
        whenever(valueConversionService.findDistinctSourceApplicationIds()).thenReturn(candidateSourceApplicationIds)
        whenever(
            userAuthorizationService.getUserAuthorizedSourceApplicationIds(
                authentication,
                candidateSourceApplicationIds,
            ),
        ).thenReturn(mockSourceApplicationIds)

        val mockContent = listOf(mock<ValueConversionResponse>())
        val mockPage = mock<Page<ValueConversionResponse>>()
        whenever(mockPage.content).thenReturn(mockContent)
        whenever(
            valueConversionService.findAllBySourceApplicationIds(
                pageRequest,
                false,
                mockSourceApplicationIds,
                ValueConversionFilter(),
            ),
        ).thenReturn(mockPage)

        val response =
            getController()
                .getValueConversions(
                    authentication = authentication,
                    filterParams =
                        validFilterParams().apply {
                            excludeConvertingMap = true
                        },
                )

        verify(valueConversionService).findDistinctSourceApplicationIds()
        verify(userAuthorizationService).getUserAuthorizedSourceApplicationIds(
            authentication,
            candidateSourceApplicationIds,
        )
        verify(valueConversionService).findAllBySourceApplicationIds(
            pageable = pageRequest,
            includeConversionMap = false,
            authorizedSourceApplicationIds = mockSourceApplicationIds,
            filter = ValueConversionFilter(),
        )

        assertThat(response.content).isEqualTo(mockContent)
    }

    @Test
    @DisplayName("passes optional filter parameters to service")
    fun `getting value conversions should pass optional filter parameters to service`() {
        val candidateSourceApplicationIds = setOf(1L, 2L, 3L)
        val authorizedSourceApplicationIds = setOf(1L, 2L)
        val createdAtFrom = Instant.parse("2026-01-01T00:00:00Z")
        val createdAtTo = Instant.parse("2026-01-31T23:59:59Z")
        val modifiedAtFrom = Instant.parse("2026-02-01T00:00:00Z")
        val modifiedAtTo = Instant.parse("2026-02-28T23:59:59Z")
        val createdBy = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val modifiedBy = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val expectedFilter =
            ValueConversionFilter(
                sourceApplicationIds = setOf(2L, 99L),
                fromTypeId = "text",
                toApplicationId = "archive",
                toTypeId = "code",
                displayName = "county",
                createdBy = createdBy,
                createdAtFrom = createdAtFrom,
                createdAtTo = createdAtTo,
                modifiedBy = modifiedBy,
                modifiedAtFrom = modifiedAtFrom,
                modifiedAtTo = modifiedAtTo,
            )
        val expectedPageRequest = PageRequest.of(1, 5, Sort.Direction.DESC, "lastModifiedAt")

        whenever(valueConversionService.findDistinctSourceApplicationIds()).thenReturn(candidateSourceApplicationIds)
        whenever(
            userAuthorizationService.getUserAuthorizedSourceApplicationIds(
                authentication,
                candidateSourceApplicationIds,
            ),
        ).thenReturn(authorizedSourceApplicationIds)

        val mockContent = listOf(mock<ValueConversionResponse>())
        val mockPage = mock<Page<ValueConversionResponse>>()
        whenever(mockPage.content).thenReturn(mockContent)
        whenever(
            valueConversionService.findAllBySourceApplicationIds(
                expectedPageRequest,
                true,
                authorizedSourceApplicationIds,
                expectedFilter,
            ),
        ).thenReturn(mockPage)

        val response =
            getController()
                .getValueConversions(
                    authentication = authentication,
                    filterParams =
                        validFilterParams().apply {
                            page = 1
                            size = 5
                            sortProperty = "modifiedAt"
                            sortDirection = Sort.Direction.DESC
                            sourceApplicationIds = setOf(2L, 99L)
                            fromTypeId = " text "
                            toApplicationId = "archive"
                            toTypeId = "code"
                            displayName = " county "
                            this.createdBy = createdBy
                            this.createdAtFrom = createdAtFrom
                            this.createdAtTo = createdAtTo
                            this.modifiedBy = modifiedBy
                            this.modifiedAtFrom = modifiedAtFrom
                            this.modifiedAtTo = modifiedAtTo
                        },
                )

        verify(valueConversionService).findAllBySourceApplicationIds(
            pageable = expectedPageRequest,
            includeConversionMap = true,
            authorizedSourceApplicationIds = authorizedSourceApplicationIds,
            filter = expectedFilter,
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
    @DisplayName("deletes value conversion if found and user has access")
    fun `deleting value conversion should delete conversion when found and user has access`() {
        whenever(valueConversionService.findById(1L)).thenReturn(validResponse(id = 1L, fromApplicationId = 2L))

        getController().deleteValueConversion(authentication, 1L)

        verify(valueConversionService).findById(1L)
        verify(userAuthorizationService).checkIfUserHasAccessToSourceApplication(authentication, 2L)
        verify(valueConversionService).delete(1L)
    }

    @Test
    @DisplayName("throws not found when deleting missing value conversion")
    fun `deleting value conversion should throw not found when conversion is missing`() {
        whenever(valueConversionService.findById(1L)).thenReturn(null)

        assertThatThrownBy { getController().deleteValueConversion(authentication, 1L) }
            .isInstanceOf(ValueConversionNotFoundException::class.java)

        verify(valueConversionService).findById(1L)
        verify(userAuthorizationService, never()).checkIfUserHasAccessToSourceApplication(any(), any())
        verify(valueConversionService, never()).delete(any())
    }

    @Test
    @DisplayName("throws forbidden when deleting value conversion without access")
    fun `deleting value conversion should throw forbidden when user has no access`() {
        doThrow(ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
            .whenever(userAuthorizationService)
            .checkIfUserHasAccessToSourceApplication(authentication, 1L)

        whenever(valueConversionService.findById(1L))
            .thenReturn(validResponse(fromApplicationId = 1L))

        assertThatThrownBy { getController().deleteValueConversion(authentication, 1L) }
            .isInstanceOf(ResponseStatusException::class.java)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN)
            .hasMessageContaining("Forbidden")

        verify(userAuthorizationService).checkIfUserHasAccessToSourceApplication(authentication, 1L)
        verify(valueConversionService).findById(1L)
        verify(valueConversionService, never()).delete(any())
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

    @Test
    @DisplayName("updates value conversion if found and user has access to existing source application")
    fun `putting value conversion should update conversion when found and user has access`() {
        val request = validRequest(fromApplicationId = 99L)
        val existingResponse = validResponse(id = 1L, fromApplicationId = 2L)
        val updatedResponse = validResponse(id = 1L, fromApplicationId = 99L)
        whenever(valueConversionService.findById(1L)).thenReturn(existingResponse)
        whenever(valueConversionService.update(1L, request)).thenReturn(updatedResponse)

        val response = getController().putValueConversion(authentication, 1L, request)

        verify(valueConversionService).findById(1L)
        verify(userAuthorizationService).checkIfUserHasAccessToSourceApplication(authentication, 2L)
        verify(valueConversionService).update(1L, request)
        assertThat(response).isEqualTo(updatedResponse)
    }

    @Test
    @DisplayName("throws not found when updating missing value conversion")
    fun `putting value conversion should throw not found when conversion is missing`() {
        val request = validRequest()
        whenever(valueConversionService.findById(1L)).thenReturn(null)

        assertThatThrownBy { getController().putValueConversion(authentication, 1L, request) }
            .isInstanceOf(ValueConversionNotFoundException::class.java)

        verify(valueConversionService).findById(1L)
        verify(userAuthorizationService, never()).checkIfUserHasAccessToSourceApplication(any(), any())
        verify(valueConversionService, never()).update(any(), any())
    }

    @Test
    @DisplayName("throws forbidden when updating value conversion without access")
    fun `putting value conversion should throw forbidden when user has no access`() {
        val request = validRequest()
        doThrow(ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
            .whenever(userAuthorizationService)
            .checkIfUserHasAccessToSourceApplication(authentication, 1L)

        whenever(valueConversionService.findById(1L))
            .thenReturn(validResponse(fromApplicationId = 1L))

        assertThatThrownBy { getController().putValueConversion(authentication, 1L, request) }
            .isInstanceOf(ResponseStatusException::class.java)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN)
            .hasMessageContaining("Forbidden")

        verify(userAuthorizationService).checkIfUserHasAccessToSourceApplication(authentication, 1L)
        verify(valueConversionService).findById(1L)
        verify(valueConversionService, never()).update(any(), any())
    }
}
