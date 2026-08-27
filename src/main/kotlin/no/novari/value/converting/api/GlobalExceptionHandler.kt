package no.novari.value.converting.api

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import io.github.oshai.kotlinlogging.KotlinLogging
import no.novari.value.converting.api.exception.InvalidRequestParameterException
import no.novari.value.converting.api.exception.ValueConversionDataIntegrityException
import no.novari.value.converting.api.exception.ValueConversionNotFoundException
import no.novari.value.converting.api.exception.ValueConversionValidationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(ValueConversionNotFoundException::class)
    fun handleValueConversionNotFound(exception: ValueConversionNotFoundException): ProblemDetail {
        logger.warn(exception) { "Value conversion not found" }
        return createProblemDetail(
            status = HttpStatus.NOT_FOUND,
            title = "Not Found",
            detail = exception.message ?: "Value conversion not found",
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(exception: HttpMessageNotReadableException): ProblemDetail {
        logger.warn(exception) { "Malformed request body" }
        return createProblemDetail(
            status = HttpStatus.UNPROCESSABLE_ENTITY,
            title = "Unprocessable Entity",
            detail = createValidationDetail(exception),
        )
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidation(exception: HandlerMethodValidationException): ProblemDetail {
        logger.warn(exception) { "Request parameter validation failed" }
        return createProblemDetail(
            status = HttpStatus.BAD_REQUEST,
            title = "Bad Request",
            detail = createMethodValidationDetail(exception),
        )
    }

    @ExceptionHandler(InvalidRequestParameterException::class)
    fun handleInvalidRequestParameter(exception: InvalidRequestParameterException): ProblemDetail {
        logger.warn(exception) { "Invalid request parameter" }
        return createProblemDetail(
            status = HttpStatus.BAD_REQUEST,
            title = "Bad Request",
            detail = exception.message ?: "Validation error: invalid request parameters",
        )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameter(exception: MissingServletRequestParameterException): ProblemDetail {
        logger.warn(exception) { "Missing request parameter" }
        return createProblemDetail(
            status = HttpStatus.BAD_REQUEST,
            title = "Bad Request",
            detail = "Validation error: '${exception.parameterName}' is required",
        )
    }

    @ExceptionHandler(BindException::class)
    fun handleBindException(exception: BindException): ProblemDetail {
        logger.warn(exception) { "Request parameter binding failed" }
        return createProblemDetail(
            status = HttpStatus.BAD_REQUEST,
            title = "Bad Request",
            detail = createBindingValidationDetail(exception),
        )
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(exception: ResponseStatusException): ProblemDetail {
        val status = HttpStatus.valueOf(exception.statusCode.value())
        logger.warn(exception) { "Request rejected with response status" }
        return createProblemDetail(
            status = status,
            title = status.reasonPhrase,
            detail = exception.reason ?: status.reasonPhrase,
        )
    }

    @ExceptionHandler(ValueConversionValidationException::class)
    fun handleValueConversionValidation(exception: ValueConversionValidationException): ProblemDetail {
        logger.warn(exception) { "Value conversion validation failed" }
        return createProblemDetail(
            status = HttpStatus.UNPROCESSABLE_ENTITY,
            title = "Unprocessable Entity",
            detail = exception.message ?: "Validation error",
        )
    }

    @ExceptionHandler(ValueConversionDataIntegrityException::class)
    fun handleValueConversionDataIntegrity(exception: ValueConversionDataIntegrityException): ProblemDetail {
        logger.error(exception) { "Value conversion data integrity violation" }
        return createProblemDetail(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            title = "Internal Server Error",
            detail = exception.message ?: "Internal data integrity violation",
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(exception: MethodArgumentTypeMismatchException): ProblemDetail {
        logger.warn(exception) { "Request parameter type mismatch" }
        return createProblemDetail(
            status = HttpStatus.BAD_REQUEST,
            title = "Bad Request",
            detail = "Invalid value for request parameter '${exception.name}'",
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnhandledException(exception: Exception): ProblemDetail {
        logger.error(exception) { "Unhandled exception" }
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

    private fun createBindingValidationDetail(exception: BindException): String {
        val fieldError = exception.bindingResult.fieldErrors.firstOrNull()
        if (fieldError != null) {
            if (fieldError.isBindingFailure) {
                return "Invalid value for request parameter '${fieldError.field}'"
            }

            val defaultMessage = fieldError.defaultMessage
            return when {
                fieldError.code == "NotNull" -> {
                    "Validation error: '${fieldError.field}' is required"
                }

                fieldError.code == "AssertTrue" && !defaultMessage.isNullOrBlank() -> {
                    "Validation error: $defaultMessage"
                }

                !defaultMessage.isNullOrBlank() -> {
                    "Validation error: '${fieldError.field}' $defaultMessage"
                }

                else -> {
                    "Validation error: invalid request parameters"
                }
            }
        }

        val objectError = exception.bindingResult.globalErrors.firstOrNull()
        val defaultMessage = objectError?.defaultMessage

        return if (!defaultMessage.isNullOrBlank()) {
            "Validation error: $defaultMessage"
        } else {
            "Validation error: invalid request parameters"
        }
    }
}
