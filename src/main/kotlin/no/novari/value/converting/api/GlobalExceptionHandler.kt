package no.novari.value.converting.api

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import no.novari.value.converting.api.exception.InvalidRequestParameterException
import no.novari.value.converting.api.exception.ValueConversionDataIntegrityException
import no.novari.value.converting.api.exception.ValueConversionNotFoundException
import no.novari.value.converting.api.exception.ValueConversionValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ValueConversionNotFoundException::class)
    fun handleValueConversionNotFound(exception: ValueConversionNotFoundException): ProblemDetail {
        logger.warn("Value conversion not found", exception)
        return createProblemDetail(
            status = HttpStatus.NOT_FOUND,
            title = "Not Found",
            detail = exception.message ?: "Value conversion not found",
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(exception: HttpMessageNotReadableException): ProblemDetail {
        logger.warn("Malformed request body", exception)
        return createProblemDetail(
            status = HttpStatus.UNPROCESSABLE_ENTITY,
            title = "Unprocessable Entity",
            detail = createValidationDetail(exception),
        )
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidation(exception: HandlerMethodValidationException): ProblemDetail {
        logger.warn("Request parameter validation failed", exception)
        return createProblemDetail(
            status = HttpStatus.BAD_REQUEST,
            title = "Bad Request",
            detail = createMethodValidationDetail(exception),
        )
    }

    @ExceptionHandler(InvalidRequestParameterException::class)
    fun handleInvalidRequestParameter(exception: InvalidRequestParameterException): ProblemDetail {
        logger.warn("Invalid request parameter", exception)
        return createProblemDetail(
            status = HttpStatus.BAD_REQUEST,
            title = "Bad Request",
            detail = exception.message ?: "Validation error: invalid request parameters",
        )
    }

    @ExceptionHandler(ValueConversionValidationException::class)
    fun handleValueConversionValidation(exception: ValueConversionValidationException): ProblemDetail {
        logger.warn("Value conversion validation failed", exception)
        return createProblemDetail(
            status = HttpStatus.UNPROCESSABLE_ENTITY,
            title = "Unprocessable Entity",
            detail = exception.message ?: "Validation error",
        )
    }

    @ExceptionHandler(ValueConversionDataIntegrityException::class)
    fun handleValueConversionDataIntegrity(exception: ValueConversionDataIntegrityException): ProblemDetail {
        logger.error("Value conversion data integrity violation", exception)
        return createProblemDetail(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            title = "Internal Server Error",
            detail = exception.message ?: "Internal data integrity violation",
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnhandledException(exception: Exception): ProblemDetail {
        logger.error("Unhandled exception", exception)
        return createProblemDetail(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            title = "Internal Server Error",
            detail = "Internal server error",
        )
    }

    private fun createProblemDetail(
        status: HttpStatus,
        title: String,
        detail: String,
    ): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(status, detail).apply {
            this.title = title
        }
    }

    private fun createValidationDetail(exception: HttpMessageNotReadableException): String {
        val mostSpecificCause = exception.mostSpecificCause
        if (mostSpecificCause is MismatchedInputException) {
            val fieldName = mostSpecificCause.path.lastOrNull()?.fieldName
            if (!fieldName.isNullOrBlank()) {
                return "Validation error: '$fieldName is required'"
            }
        }
        return "Validation error: malformed request body"
    }

    private fun createMethodValidationDetail(exception: HandlerMethodValidationException): String {
        val validationResult = exception.parameterValidationResults.firstOrNull()
        val error = validationResult?.resolvableErrors?.firstOrNull()
        val parameterName = validationResult?.methodParameter?.parameterName
        val defaultMessage = error?.defaultMessage

        return if (!parameterName.isNullOrBlank() && !defaultMessage.isNullOrBlank()) {
            "Validation error: '$parameterName' $defaultMessage"
        } else {
            "Validation error: invalid request parameters"
        }
    }
}
