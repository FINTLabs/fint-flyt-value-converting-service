package no.novari.value.converting.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import no.novari.value.converting.api.dto.ValueConversionRequest
import no.novari.value.converting.api.dto.ValueConversionResponse
import no.novari.value.converting.api.exception.ValueConversionValidationException
import no.novari.value.converting.application.ValueConversionService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.core.Authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

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
    fun `getting value conversions with unknown sort property should return internal server error problem detail`() {
        whenever(userAuthorizationService.getUserAuthorizedSourceApplicationIds(authentication)).thenReturn(setOf(1L))
        whenever(valueConversionService.findAllBySourceApplicationIds(any(), any(), any()))
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
