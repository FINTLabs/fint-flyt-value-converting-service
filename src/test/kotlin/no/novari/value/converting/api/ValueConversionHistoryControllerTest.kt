package no.novari.value.converting.api

import no.novari.flyt.audit.history.AuditPropertyFilter
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import no.novari.value.converting.api.exception.ValueConversionNotFoundException
import no.novari.value.converting.domain.ValueConversion
import no.novari.value.converting.domain.ValueConversionHistoryService
import no.novari.value.converting.infrastructure.persistence.ValueConversionRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ValueConversionHistoryControllerTest {
    @Mock
    private lateinit var valueConversionRepository: ValueConversionRepository

    @Mock
    private lateinit var userAuthorizationService: UserAuthorizationService

    @Mock
    private lateinit var valueConversionHistoryService: ValueConversionHistoryService

    @Mock
    private lateinit var authentication: Authentication

    private fun getController(): ValueConversionHistoryController {
        return ValueConversionHistoryController(
            valueConversionRepository,
            userAuthorizationService,
            valueConversionHistoryService,
        )
    }

    @Test
    fun `checkAccess should pass through to userAuthorizationService when value conversion exists`() {
        val valueConversion = ValueConversion(id = 1L, fromApplicationId = 42L)
        whenever(valueConversionRepository.findById(1L)).thenReturn(Optional.of(valueConversion))

        getController().checkAccess(authentication, 1L)

        org.mockito.kotlin
            .verify(userAuthorizationService)
            .checkIfUserHasAccessToSourceApplication(authentication, 42L)
    }

    @Test
    fun `checkAccess should throw not found when value conversion is missing`() {
        whenever(valueConversionRepository.findById(1L)).thenReturn(Optional.empty())

        assertThatThrownBy { getController().checkAccess(authentication, 1L) }
            .isInstanceOf(ValueConversionNotFoundException::class.java)
    }

    @Test
    fun `checkAccess should propagate forbidden from userAuthorizationService`() {
        val valueConversion = ValueConversion(id = 1L, fromApplicationId = 42L)
        whenever(valueConversionRepository.findById(1L)).thenReturn(Optional.of(valueConversion))
        doThrow(ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
            .whenever(userAuthorizationService)
            .checkIfUserHasAccessToSourceApplication(authentication, 42L)

        assertThatThrownBy { getController().checkAccess(authentication, 1L) }
            .isInstanceOf(ResponseStatusException::class.java)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN)
    }

    @Test
    fun `additionalFilter should scope by users authorized source application ids`() {
        val sourceApplicationIds = setOf(1L, 2L)
        whenever(userAuthorizationService.getUserAuthorizedSourceApplicationIds(authentication))
            .thenReturn(sourceApplicationIds)

        val filter = getController().additionalFilter(authentication)

        assertThat(filter).isEqualTo(AuditPropertyFilter("fromApplicationId", sourceApplicationIds))
    }
}
