package no.novari.value.converting.api

import jakarta.validation.Valid
import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_API
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import no.novari.value.converting.api.dto.ValueConversionFilterParams
import no.novari.value.converting.api.dto.ValueConversionPageResponse
import no.novari.value.converting.api.dto.ValueConversionRequest
import no.novari.value.converting.api.dto.ValueConversionResponse
import no.novari.value.converting.api.exception.ValueConversionNotFoundException
import no.novari.value.converting.application.ValueConversionService
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
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
        authentication: Authentication,
        @Valid @ModelAttribute filterParams: ValueConversionFilterParams,
    ): ValueConversionPageResponse {
        val pageRequest = filterParams.toPageRequest()
        val filter = filterParams.toFilter()

        val sourceApplicationIds =
            userAuthorizationService
                .getUserAuthorizedSourceApplicationIds(
                    authentication,
                    valueConversionService.findDistinctSourceApplicationIds(),
                )

        val valueConversions =
            valueConversionService.findAllBySourceApplicationIds(
                pageable = pageRequest,
                includeConversionMap = !filterParams.excludeConvertingMap,
                authorizedSourceApplicationIds = sourceApplicationIds,
                filter = filter,
            )

        return ValueConversionPageResponse(content = valueConversions.content)
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
