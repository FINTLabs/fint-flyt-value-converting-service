package no.novari.value.converting.api

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_API
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import no.novari.value.converting.api.dto.ValueConversionPageResponse
import no.novari.value.converting.api.dto.ValueConversionRequest
import no.novari.value.converting.api.dto.ValueConversionResponse
import no.novari.value.converting.api.exception.InvalidRequestParameterException
import no.novari.value.converting.api.exception.ValueConversionNotFoundException
import no.novari.value.converting.application.ValueConversionFilter
import no.novari.value.converting.application.ValueConversionService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@Validated
@RequestMapping("$INTERNAL_API/value-convertings")
class ValueConversionController(
    private val valueConversionService: ValueConversionService,
    private val userAuthorizationService: UserAuthorizationService,
) {
    @GetMapping
    fun getValueConversions(
        authentication: Authentication,
        @RequestParam @Min(0) page: Int,
        @RequestParam @Min(1) @Max(1000) size: Int,
        @RequestParam sortProperty: String,
        @RequestParam sortDirection: Sort.Direction,
        @RequestParam(name = "excludeConvertingMap", required = false, defaultValue = "false") excludeConversionMap:
            Boolean,
        @RequestParam(name = "sourceApplicationIds", required = false) requestedSourceApplicationIds: Set<Long>? = null,
        @RequestParam(required = false) fromTypeId: String? = null,
        @RequestParam(required = false) toApplicationId: String? = null,
        @RequestParam(required = false) toTypeId: String? = null,
        @RequestParam(required = false) displayName: String? = null,
        @RequestParam(required = false) createdBy: String? = null,
        @RequestParam(required = false) createdAtFrom: Instant? = null,
        @RequestParam(required = false) createdAtTo: Instant? = null,
        @RequestParam(required = false) modifiedBy: String? = null,
        @RequestParam(required = false) lastModifiedBy: String? = null,
        @RequestParam(required = false) modifiedAtFrom: Instant? = null,
        @RequestParam(required = false) modifiedAtTo: Instant? = null,
        @RequestParam(required = false) lastModifiedAtFrom: Instant? = null,
        @RequestParam(required = false) lastModifiedAtTo: Instant? = null,
    ): ValueConversionPageResponse {
        validatePage(page)
        validateSize(size)

        val pageRequest =
            PageRequest
                .of(page, size)
                .withSort(sortDirection, sortProperty.toEntitySortProperty())

        val sourceApplicationIds =
            userAuthorizationService
                .getUserAuthorizedSourceApplicationIds(
                    authentication,
                    valueConversionService.findDistinctSourceApplicationIds(),
                )

        val valueConversions =
            valueConversionService.findAllBySourceApplicationIds(
                pageable = pageRequest,
                includeConversionMap = !excludeConversionMap,
                sourceApplicationIds = sourceApplicationIds,
                filter =
                    ValueConversionFilter(
                        sourceApplicationIds = requestedSourceApplicationIds.orEmpty(),
                        fromTypeId = fromTypeId,
                        toApplicationId = toApplicationId,
                        toTypeId = toTypeId,
                        displayName = displayName,
                        createdBy = createdBy,
                        createdAtFrom = createdAtFrom,
                        createdAtTo = createdAtTo,
                        modifiedBy = modifiedBy ?: lastModifiedBy,
                        modifiedAtFrom = modifiedAtFrom ?: lastModifiedAtFrom,
                        modifiedAtTo = modifiedAtTo ?: lastModifiedAtTo,
                    ),
            )

        return ValueConversionPageResponse(content = valueConversions.content)
    }

    private fun String.toEntitySortProperty(): String =
        when (this) {
            "modifiedBy" -> "lastModifiedBy"
            "modifiedAt" -> "lastModifiedAt"
            else -> this
        }

    private fun validateSize(size: Int) {
        if (size < 1) {
            throw InvalidRequestParameterException("Validation error: 'size' must be greater than or equal to 1")
        }
        if (size > 1000) {
            throw InvalidRequestParameterException("Validation error: 'size' must be less than or equal to 1000")
        }
    }

    private fun validatePage(page: Int) {
        if (page < 0) {
            throw InvalidRequestParameterException("Validation error: 'page' must be greater than or equal to 0")
        }
    }

    @GetMapping("{valueConversionId}")
    fun getValueConversion(
        authentication: Authentication,
        @PathVariable valueConversionId: Long,
    ): ValueConversionResponse {
        val valueConversion =
            valueConversionService.findById(valueConversionId)
                ?: throw ValueConversionNotFoundException(valueConversionId)

        userAuthorizationService.checkIfUserHasAccessToSourceApplication(
            authentication,
            valueConversion.fromApplicationId,
        )

        return valueConversion
    }

    @PostMapping
    fun postValueConversion(
        authentication: Authentication,
        @Valid @RequestBody valueConversionRequest: ValueConversionRequest,
    ): ValueConversionResponse {
        userAuthorizationService.checkIfUserHasAccessToSourceApplication(
            authentication,
            valueConversionRequest.fromApplicationId,
        )

        return valueConversionService.save(valueConversionRequest)
    }

    @PutMapping("{valueConversionId}")
    fun putValueConversion(
        authentication: Authentication,
        @PathVariable valueConversionId: Long,
        @Valid @RequestBody valueConversionRequest: ValueConversionRequest,
    ): ValueConversionResponse {
        val valueConversion =
            valueConversionService.findById(valueConversionId)
                ?: throw ValueConversionNotFoundException(valueConversionId)

        userAuthorizationService.checkIfUserHasAccessToSourceApplication(
            authentication,
            valueConversion.fromApplicationId,
        )

        return valueConversionService.update(valueConversionId, valueConversionRequest)
    }

    @DeleteMapping("{valueConversionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteValueConversion(
        authentication: Authentication,
        @PathVariable valueConversionId: Long,
    ) {
        val valueConversion =
            valueConversionService.findById(valueConversionId)
                ?: throw ValueConversionNotFoundException(valueConversionId)

        userAuthorizationService.checkIfUserHasAccessToSourceApplication(
            authentication,
            valueConversion.fromApplicationId,
        )

        valueConversionService.delete(valueConversionId)
    }
}
