package no.novari.value.converting.api

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_API
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import no.novari.value.converting.api.dto.ValueConversionRequest
import no.novari.value.converting.api.dto.ValueConversionResponse
import no.novari.value.converting.api.exception.InvalidRequestParameterException
import no.novari.value.converting.api.exception.ValueConversionNotFoundException
import no.novari.value.converting.application.ValueConversionService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
@RequestMapping("$INTERNAL_API/value-convertings")
class ValueConversionController(
    private val valueConversionService: ValueConversionService,
    private val userAuthorizationService: UserAuthorizationService,
) {
    @GetMapping
    fun getValueConversions(
        @AuthenticationPrincipal authentication: Authentication,
        @RequestParam @Min(0) page: Int,
        @RequestParam @Min(1) @Max(1000) size: Int,
        @RequestParam sortProperty: String,
        @RequestParam sortDirection: Sort.Direction,
        @RequestParam(name = "excludeConvertingMap", required = false, defaultValue = "false") excludeConversionMap:
            Boolean,
    ): Page<ValueConversionResponse> {
        validatePage(page)
        validateSize(size)

        val pageRequest =
            PageRequest
                .of(page, size)
                .withSort(sortDirection, sortProperty)

        val sourceApplicationIds =
            userAuthorizationService
                .getUserAuthorizedSourceApplicationIds(authentication)

        return valueConversionService.findAllBySourceApplicationIds(
            pageable = pageRequest,
            includeConversionMap = !excludeConversionMap,
            sourceApplicationIds = sourceApplicationIds,
        )
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
        @AuthenticationPrincipal authentication: Authentication,
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
        @AuthenticationPrincipal authentication: Authentication,
        @Valid @RequestBody valueConversionRequest: ValueConversionRequest,
    ): ValueConversionResponse {
        userAuthorizationService.checkIfUserHasAccessToSourceApplication(
            authentication,
            valueConversionRequest.fromApplicationId,
        )

        return valueConversionService.save(valueConversionRequest)
    }
}
