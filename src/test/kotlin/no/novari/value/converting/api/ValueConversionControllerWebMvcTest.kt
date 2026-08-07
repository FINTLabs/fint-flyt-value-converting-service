package no.novari.value.converting.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import no.novari.value.converting.api.dto.ValueConversionRequest
import no.novari.value.converting.api.dto.ValueConversionResponse
import no.novari.value.converting.api.exception.ValueConversionValidationException
import no.novari.value.converting.application.ValueConversionFilter
import no.novari.value.converting.application.ValueConversionService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.core.Authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class ValueConversionControllerWebMvcTest {
    @Mock
    private lateinit var valueConversionService: ValueConversionService

    @Mock
    private lateinit var userAuthorizationService: UserAuthorizationService

    @Mock
    private lateinit var authentication: Authentication

    private val objectMapper = jacksonObjectMapper()

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val controller = ValueConversionController(valueConversionService, userAuthorizationService)
        val validator =
            LocalValidatorFactoryBean().apply {
                afterPropertiesSet()
            }

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .setMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build()
    }

    @Test
    fun `posting value conversion should return success response`() {
        val request = validRequest()
        val response = validResponse()
        whenever(valueConversionService.save(request)).thenReturn(response)

        mockMvc
            .perform(
                post("/api/intern/value-convertings")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.displayName").value("Display name"))
            .andExpect(jsonPath("$.fromApplicationId").value(1))
            .andExpect(jsonPath("$.convertingMap.A").value("B"))

        verify(userAuthorizationService).checkIfUserHasAccessToSourceApplication(authentication, 1L)
        verify(valueConversionService).save(request)
    }

    @Test
    fun `posting value conversion with missing required field should return unprocessable entity problem detail`() {
        val requestBody =
            """
            {
              "fromApplicationId": 1,
              "fromTypeId": "fromType",
              "toApplicationId": "toAppId",
              "toTypeId": "toType",
              "convertingMap": {"A":"B"}
            }
            """.trimIndent()

        mockMvc
            .perform(
                post("/api/intern/value-convertings")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody),
            ).andExpect(status().isUnprocessableEntity)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Unprocessable Entity"))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.detail").value("Validation error: 'displayName is required'"))

        verifyNoInteractions(userAuthorizationService, valueConversionService)
    }

    @Test
    fun `posting value conversion should ignore unknown fields`() {
        val requestBody =
            """
            {
              "displayName": "Display name",
              "fromApplicationId": 1,
              "fromTypeId": "fromType",
              "toApplicationId": "toAppId",
              "toTypeId": "toType",
              "convertingMap": {"A":"B"},
              "id": 999,
              "unknownField": "ignored"
            }
            """.trimIndent()

        val expectedRequest = validRequest()
        whenever(valueConversionService.save(expectedRequest)).thenReturn(validResponse())

        mockMvc
            .perform(
                post("/api/intern/value-convertings")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody),
            ).andExpect(status().isOk)

        verify(userAuthorizationService).checkIfUserHasAccessToSourceApplication(authentication, 1L)
        verify(valueConversionService).save(expectedRequest)
    }

    @Test
    fun `posting value conversion with duplicate trimmed keys should return unprocessable entity problem detail`() {
        val request = validRequest()
        whenever(valueConversionService.save(request))
            .thenThrow(
                ValueConversionValidationException(
                    "Validation error: convertingMap contains duplicate keys after trimming",
                ),
            )

        mockMvc
            .perform(
                post("/api/intern/value-convertings")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isUnprocessableEntity)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Unprocessable Entity"))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(
                jsonPath("$.detail").value(
                    "Validation error: convertingMap contains duplicate keys after trimming",
                ),
            )
    }

    @Test
    fun `putting value conversion should return updated response`() {
        val request =
            ValueConversionRequest(
                displayName = "Updated display name",
                fromApplicationId = 2L,
                fromTypeId = "updatedFromType",
                toApplicationId = "updatedToAppId",
                toTypeId = "updatedToType",
                convertingMap = mapOf("C" to "D"),
            )
        val existingResponse = validResponse()
        val updatedResponse =
            ValueConversionResponse(
                id = 1L,
                displayName = "Updated display name",
                fromApplicationId = 2L,
                fromTypeId = "updatedFromType",
                toApplicationId = "updatedToAppId",
                toTypeId = "updatedToType",
                convertingMap = mapOf("C" to "D"),
            )
        whenever(valueConversionService.findById(1L)).thenReturn(existingResponse)
        whenever(valueConversionService.update(1L, request)).thenReturn(updatedResponse)

        mockMvc
            .perform(
                put("/api/intern/value-convertings/1")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.displayName").value("Updated display name"))
            .andExpect(jsonPath("$.fromApplicationId").value(2))
            .andExpect(jsonPath("$.convertingMap.C").value("D"))

        verify(userAuthorizationService).checkIfUserHasAccessToSourceApplication(authentication, 1L)
        verify(valueConversionService).update(1L, request)
    }

    @Test
    fun `putting value conversion with unknown id should return not found problem detail`() {
        val request = validRequest()
        whenever(valueConversionService.findById(123L)).thenReturn(null)

        mockMvc
            .perform(
                put("/api/intern/value-convertings/123")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Not Found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("Value conversion with id=123 was not found"))

        verify(valueConversionService, never()).update(any(), any())
    }

    @Test
    fun `putting value conversion without source application access should return forbidden problem detail`() {
        val request = validRequest()
        whenever(valueConversionService.findById(1L)).thenReturn(validResponse())
        doThrow(
            ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access or modify data that is related to source application with id=1",
            ),
        ).whenever(userAuthorizationService)
            .checkIfUserHasAccessToSourceApplication(authentication, 1L)

        mockMvc
            .perform(
                put("/api/intern/value-convertings/1")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isForbidden)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Forbidden"))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(
                jsonPath("$.detail")
                    .value(
                        "You do not have permission to access or modify data that is related to source application " +
                            "with id=1",
                    ),
            )

        verify(valueConversionService, never()).update(any(), any())
    }

    @Test
    fun `putting value conversion with duplicate trimmed keys should return unprocessable entity problem detail`() {
        val request =
            ValueConversionRequest(
                displayName = "Display name",
                fromApplicationId = 1L,
                fromTypeId = "fromType",
                toApplicationId = "toAppId",
                toTypeId = "toType",
                convertingMap = mapOf("A " to "B", " A" to "C"),
            )
        whenever(valueConversionService.findById(1L)).thenReturn(validResponse())
        whenever(valueConversionService.update(1L, request))
            .thenThrow(
                ValueConversionValidationException(
                    "Validation error: convertingMap contains duplicate keys after trimming",
                ),
            )

        mockMvc
            .perform(
                put("/api/intern/value-convertings/1")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isUnprocessableEntity)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Unprocessable Entity"))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(
                jsonPath("$.detail").value(
                    "Validation error: convertingMap contains duplicate keys after trimming",
                ),
            )
    }

    @Test
    fun `getting value conversion with unknown id should return not found problem detail`() {
        whenever(valueConversionService.findById(123L)).thenReturn(null)

        mockMvc
            .perform(
                get("/api/intern/value-convertings/123")
                    .principal(authentication),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Not Found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("Value conversion with id=123 was not found"))
    }

    @Test
    fun `deleting value conversion should return no content`() {
        whenever(valueConversionService.findById(1L)).thenReturn(validResponse())

        mockMvc
            .perform(
                delete("/api/intern/value-convertings/1")
                    .principal(authentication),
            ).andExpect(status().isNoContent)
            .andExpect(content().string(""))

        verify(userAuthorizationService).checkIfUserHasAccessToSourceApplication(authentication, 1L)
        verify(valueConversionService).delete(1L)
    }

    @Test
    fun `deleting value conversion with unknown id should return not found problem detail`() {
        whenever(valueConversionService.findById(123L)).thenReturn(null)

        mockMvc
            .perform(
                delete("/api/intern/value-convertings/123")
                    .principal(authentication),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Not Found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("Value conversion with id=123 was not found"))

        verify(valueConversionService, never()).delete(any())
    }

    @Test
    fun `deleting value conversion without source application access should return forbidden problem detail`() {
        whenever(valueConversionService.findById(1L)).thenReturn(validResponse())
        doThrow(
            ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access or modify data that is related to source application with id=1",
            ),
        ).whenever(userAuthorizationService)
            .checkIfUserHasAccessToSourceApplication(authentication, 1L)

        mockMvc
            .perform(
                delete("/api/intern/value-convertings/1")
                    .principal(authentication),
            ).andExpect(status().isForbidden)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Forbidden"))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(
                jsonPath("$.detail")
                    .value(
                        "You do not have permission to access or modify data that is related to source application " +
                            "with id=1",
                    ),
            )

        verify(valueConversionService, never()).delete(any())
    }

    @Test
    fun `getting value conversions with filters should return success response`() {
        val candidateSourceApplicationIds = setOf(1L, 2L, 3L)
        val authorizedSourceApplicationIds = setOf(2L, 3L)
        val createdAtFrom = Instant.parse("2026-01-01T00:00:00Z")
        val createdAtTo = Instant.parse("2026-01-31T23:59:59Z")
        val modifiedAtFrom = Instant.parse("2026-02-01T00:00:00Z")
        val modifiedAtTo = Instant.parse("2026-02-28T23:59:59Z")
        val expectedPageRequest = PageRequest.of(0, 20, Sort.Direction.ASC, "lastModifiedBy")
        val expectedFilter =
            ValueConversionFilter(
                sourceApplicationIds = setOf(2L, 99L),
                fromTypeId = "text",
                toApplicationId = "archive",
                toTypeId = "code",
                displayName = "Display",
                createdBy = "creator",
                createdAtFrom = createdAtFrom,
                createdAtTo = createdAtTo,
                modifiedBy = "modifier",
                modifiedAtFrom = modifiedAtFrom,
                modifiedAtTo = modifiedAtTo,
            )

        whenever(valueConversionService.findDistinctSourceApplicationIds()).thenReturn(candidateSourceApplicationIds)
        whenever(
            userAuthorizationService.getUserAuthorizedSourceApplicationIds(
                authentication,
                candidateSourceApplicationIds,
            ),
        ).thenReturn(authorizedSourceApplicationIds)
        whenever(
            valueConversionService.findAllBySourceApplicationIds(
                expectedPageRequest,
                false,
                authorizedSourceApplicationIds,
                expectedFilter,
            ),
        ).thenReturn(PageImpl(listOf(validResponse())))

        mockMvc
            .perform(
                get("/api/intern/value-convertings")
                    .principal(authentication)
                    .queryParam("page", "0")
                    .queryParam("size", "20")
                    .queryParam("sortProperty", "modifiedBy")
                    .queryParam("sortDirection", "ASC")
                    .queryParam("excludeConvertingMap", "true")
                    .queryParam("sourceApplicationIds", "2", "99")
                    .queryParam("fromTypeId", "text")
                    .queryParam("toApplicationId", "archive")
                    .queryParam("toTypeId", "code")
                    .queryParam("displayName", "Display")
                    .queryParam("createdBy", "creator")
                    .queryParam("createdAtFrom", createdAtFrom.toString())
                    .queryParam("createdAtTo", createdAtTo.toString())
                    .queryParam("modifiedBy", "modifier")
                    .queryParam("modifiedAtFrom", modifiedAtFrom.toString())
                    .queryParam("modifiedAtTo", modifiedAtTo.toString()),
            ).andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].displayName").value("Display name"))

        verify(valueConversionService).findAllBySourceApplicationIds(
            expectedPageRequest,
            false,
            authorizedSourceApplicationIds,
            expectedFilter,
        )
    }

    @Test
    fun `getting value conversions with invalid size should return bad request problem detail`() {
        mockMvc
            .perform(
                get("/api/intern/value-convertings")
                    .principal(authentication)
                    .queryParam("page", "0")
                    .queryParam("size", "0")
                    .queryParam("sortProperty", "id")
                    .queryParam("sortDirection", "ASC"),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("Validation error: 'size' must be greater than or equal to 1"))

        verifyNoInteractions(userAuthorizationService, valueConversionService)
    }

    @Test
    fun `getting value conversions without required page should return bad request problem detail`() {
        mockMvc
            .perform(
                get("/api/intern/value-convertings")
                    .principal(authentication)
                    .queryParam("size", "10")
                    .queryParam("sortProperty", "id")
                    .queryParam("sortDirection", "ASC"),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("Validation error: 'page' is required"))

        verifyNoInteractions(userAuthorizationService, valueConversionService)
    }

    @Test
    fun `getting value conversions with unknown sort property should return internal server error problem detail`() {
        val candidateSourceApplicationIds = setOf(1L)
        whenever(valueConversionService.findDistinctSourceApplicationIds()).thenReturn(candidateSourceApplicationIds)
        whenever(
            userAuthorizationService.getUserAuthorizedSourceApplicationIds(
                authentication,
                candidateSourceApplicationIds,
            ),
        ).thenReturn(candidateSourceApplicationIds)
        whenever(valueConversionService.findAllBySourceApplicationIds(any(), any(), any(), any()))
            .thenThrow(
                IllegalArgumentException(
                    "No property 'unknownField' found for type 'ValueConversion'",
                ),
            )

        mockMvc
            .perform(
                get("/api/intern/value-convertings")
                    .principal(authentication)
                    .queryParam("page", "0")
                    .queryParam("size", "10")
                    .queryParam("sortProperty", "unknownField")
                    .queryParam("sortDirection", "ASC"),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Internal Server Error"))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.detail").value("Internal server error"))
    }

    private fun validRequest(): ValueConversionRequest {
        return ValueConversionRequest(
            displayName = "Display name",
            fromApplicationId = 1L,
            fromTypeId = "fromType",
            toApplicationId = "toAppId",
            toTypeId = "toType",
            convertingMap = mapOf("A" to "B"),
        )
    }

    private fun validResponse(): ValueConversionResponse {
        return ValueConversionResponse(
            id = 1L,
            displayName = "Display name",
            fromApplicationId = 1L,
            fromTypeId = "fromType",
            toApplicationId = "toAppId",
            toTypeId = "toType",
            convertingMap = mapOf("A" to "B"),
        )
    }
}
