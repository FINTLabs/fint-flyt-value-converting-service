package no.novari.value.converting.api

import no.novari.flyt.audit.history.AuditPropertyFilter
import no.novari.flyt.audit.web.HistoryControllerSupport
import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_API
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import no.novari.value.converting.api.dto.ValueConversionResponse
import no.novari.value.converting.api.exception.ValueConversionNotFoundException
import no.novari.value.converting.domain.ValueConversion
import no.novari.value.converting.domain.ValueConversionHistoryService
import no.novari.value.converting.infrastructure.persistence.ValueConversionRepository
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("$INTERNAL_API/value-convertings")
class ValueConversionHistoryController(
    private val valueConversionRepository: ValueConversionRepository,
    private val userAuthorizationService: UserAuthorizationService,
    historyService: ValueConversionHistoryService,
) : HistoryControllerSupport<ValueConversion, Long, ValueConversionResponse>(historyService) {
    public override fun checkAccess(
        authentication: Authentication,
        id: Long,
    ) {
        val valueConversion =
            valueConversionRepository
                .findById(id)
                .orElseThrow { ValueConversionNotFoundException(id) }
        userAuthorizationService.checkIfUserHasAccessToSourceApplication(
            authentication,
            checkNotNull(valueConversion.fromApplicationId),
        )
    }

    public override fun additionalFilter(authentication: Authentication): AuditPropertyFilter =
        AuditPropertyFilter(
            property = "fromApplicationId",
            allowedValues = userAuthorizationService.getUserAuthorizedSourceApplicationIds(authentication),
        )
}
