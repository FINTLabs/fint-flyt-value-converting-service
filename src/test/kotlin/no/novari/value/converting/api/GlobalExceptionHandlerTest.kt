package no.novari.value.converting.api

import no.novari.value.converting.api.exception.InvalidRequestParameterException
import no.novari.value.converting.api.exception.ValueConversionDataIntegrityException
import no.novari.value.converting.api.exception.ValueConversionNotFoundException
import no.novari.value.converting.api.exception.ValueConversionValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage

class GlobalExceptionHandlerTest {
    private val globalExceptionHandler = GlobalExceptionHandler()

    @Test
    fun `handling not found should return not found problem detail`() {
        val exception = ValueConversionNotFoundException(123L)

        val problemDetail = globalExceptionHandler.handleValueConversionNotFound(exception)

        assertEquals(HttpStatus.NOT_FOUND.value(), problemDetail.status)
        assertEquals("Not Found", problemDetail.title)
        assertEquals("Value conversion with id=123 was not found", problemDetail.detail)
    }

    @Test
    fun `handling unreadable request body should return unprocessable entity problem detail`() {
        val exception =
            HttpMessageNotReadableException(
                "Invalid request body",
                RuntimeException("Malformed JSON"),
                MockHttpInputMessage(byteArrayOf()),
            )

        val problemDetail = globalExceptionHandler.handleHttpMessageNotReadable(exception)

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), problemDetail.status)
        assertEquals("Unprocessable Entity", problemDetail.title)
        assertEquals("Validation error: malformed request body", problemDetail.detail)
    }

    @Test
    fun `handling data integrity violation should return internal server error problem detail`() {
        val exception = ValueConversionDataIntegrityException(123L, "displayName")

        val problemDetail = globalExceptionHandler.handleValueConversionDataIntegrity(exception)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemDetail.status)
        assertEquals("Internal Server Error", problemDetail.title)
        assertEquals(
            "Value conversion data integrity violation: field 'displayName' was null for valueConversionId=123",
            problemDetail.detail,
        )
    }

    @Test
    fun `handling value conversion validation should return unprocessable entity problem detail`() {
        val exception =
            ValueConversionValidationException(
                "Validation error: convertingMap contains duplicate keys after trimming",
            )

        val problemDetail = globalExceptionHandler.handleValueConversionValidation(exception)

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), problemDetail.status)
        assertEquals("Unprocessable Entity", problemDetail.title)
        assertEquals(
            "Validation error: convertingMap contains duplicate keys after trimming",
            problemDetail.detail,
        )
    }

    @Test
    fun `handling unhandled exception should return generic internal server error problem detail`() {
        val exception = RuntimeException("Sensitive internal details")

        val problemDetail = globalExceptionHandler.handleUnhandledException(exception)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemDetail.status)
        assertEquals("Internal Server Error", problemDetail.title)
        assertEquals("Internal server error", problemDetail.detail)
    }

    @Test
    fun `handling invalid request parameter should return bad request problem detail`() {
        val exception = InvalidRequestParameterException("Validation error: 'size' must be greater than or equal to 1")

        val problemDetail = globalExceptionHandler.handleInvalidRequestParameter(exception)

        assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.status)
        assertEquals("Bad Request", problemDetail.title)
        assertEquals("Validation error: 'size' must be greater than or equal to 1", problemDetail.detail)
    }
}
