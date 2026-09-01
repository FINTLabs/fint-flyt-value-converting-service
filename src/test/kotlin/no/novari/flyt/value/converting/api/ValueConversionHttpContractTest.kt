package no.novari.flyt.value.converting.api

import no.novari.flyt.audit.actor.Actor
import no.novari.flyt.audit.history.EntityHistoryEntryDto
import no.novari.flyt.audit.history.HistoryEntryDto
import no.novari.flyt.audit.history.HistoryEventType
import no.novari.flyt.catalog.contract.fixtures.CatalogContractFixtures
import no.novari.flyt.catalog.contract.fixtures.FixtureObjectMapper
import no.novari.flyt.catalog.contract.fixtures.HttpContractFixture
import no.novari.flyt.catalog.contract.fixtures.HttpContractFixtureRunner
import no.novari.flyt.value.converting.api.dto.ValueConversionResponse
import no.novari.flyt.value.converting.api.dto.ValueConversionSnapshot
import no.novari.flyt.value.converting.api.exception.ValueConversionValidationException
import no.novari.flyt.value.converting.application.ValueConversionService
import no.novari.flyt.value.converting.domain.ValueConversion
import no.novari.flyt.value.converting.domain.ValueConversionHistoryService
import no.novari.flyt.value.converting.infrastructure.persistence.ValueConversionRepository
import no.novari.flyt.webresourceserver.security.user.UserAuthorizationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.HttpStatus
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.core.Authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.Optional
import java.util.UUID

/**
 * Fastholder HTTP-kontrakten slik den er i dag, mot de delte fixturene i
 * `no.novari:flyt-catalog-contract-fixtures`. Katalogtjenesten kjører de samme fixturene med sitt
 * eget oppsett, slik at et avvik mellom de to tjenestene feiler i test framfor i drift.
 *
 * Kilden er hele fixture-settet for domenet, ikke en håndplukket liste: en ny fixture uten oppsett
 * i [stubServiceLayerFor] feiler umiddelbart, i begge tjenester.
 */
class ValueConversionHttpContractTest {
    private lateinit var valueConversionService: ValueConversionService
    private lateinit var valueConversionRepository: ValueConversionRepository
    private lateinit var userAuthorizationService: UserAuthorizationService
    private lateinit var valueConversionHistoryService: ValueConversionHistoryService
    private lateinit var authentication: Authentication
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        valueConversionService = mock()
        valueConversionRepository = mock()
        userAuthorizationService = mock()
        valueConversionHistoryService = mock()
        authentication = mock()

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(
                    ValueConversionController(valueConversionService, userAuthorizationService),
                    ValueConversionHistoryController(
                        valueConversionRepository,
                        userAuthorizationService,
                        valueConversionHistoryService,
                    ),
                ).setControllerAdvice(GlobalExceptionHandler())
                .setCustomArgumentResolvers(PageableHandlerMethodArgumentResolver())
                .setMessageConverters(MappingJackson2HttpMessageConverter(OBJECT_MAPPER))
                .setValidator(LocalValidatorFactoryBean().apply { afterPropertiesSet() })
                .build()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("httpContractFixtures")
    fun `HTTP-kontrakten er uendret`(fixture: HttpContractFixture) {
        stubServiceLayerFor(fixture)

        HttpContractFixtureRunner(
            mockMvc = mockMvc,
            objectMapper = OBJECT_MAPPER,
            customizeRequest = { it.principal(authentication) },
        ).verify(fixture)
    }

    private fun stubServiceLayerFor(fixture: HttpContractFixture) {
        when (fixture.id) {
            "value-converting/list/ok-default-sort",
            "value-converting/list/ok-filtered",
            -> {
                stubList(response())
            }

            "value-converting/list/ok-exclude-converting-map" -> {
                stubList(response(convertingMap = null))
            }

            "value-converting/list/bad-request-missing-page",
            "value-converting/list/bad-request-size-below-minimum",
            "value-converting/list/bad-request-unknown-sort-property",
            "value-converting/list/bad-request-invalid-instant",
            "value-converting/list/bad-request-inverted-created-range",
            "value-converting/post/unprocessable-missing-display-name",
            -> {
                requestIsRejectedBeforeServiceLayer()
            }

            "value-converting/get-by-id/ok" -> {
                whenever(valueConversionService.findById(1L)).thenReturn(response())
            }

            "value-converting/get-by-id/not-found",
            "value-converting/delete/not-found",
            -> {
                whenever(valueConversionService.findById(123L)).thenReturn(null)
            }

            "value-converting/get-by-id/forbidden",
            "value-converting/delete/forbidden",
            "value-converting/put/forbidden",
            -> {
                whenever(valueConversionService.findById(1L)).thenReturn(response())
                denyAccessToSourceApplication(1L)
            }

            "value-converting/post/ok",
            "value-converting/post/ignores-unknown-fields",
            -> {
                whenever(valueConversionService.save(any())).thenReturn(response())
            }

            "value-converting/post/unprocessable-duplicate-trimmed-keys" -> {
                whenever(valueConversionService.save(any())).thenThrow(duplicateTrimmedKeys())
            }

            "value-converting/post/forbidden" -> {
                denyAccessToSourceApplication(1L)
            }

            "value-converting/put/ok" -> {
                whenever(valueConversionService.findById(1L)).thenReturn(response())
                whenever(valueConversionService.update(eq(1L), any())).thenReturn(
                    response(
                        displayName = "Updated display name",
                        fromApplicationId = 2L,
                        fromTypeId = "updatedFromType",
                        toApplicationId = "updatedToAppId",
                        toTypeId = "updatedToType",
                        convertingMap = mapOf("C" to "D"),
                    ),
                )
            }

            "value-converting/put/not-found" -> {
                whenever(valueConversionService.findById(123L)).thenReturn(null)
            }

            "value-converting/put/unprocessable-duplicate-trimmed-keys" -> {
                whenever(valueConversionService.findById(1L)).thenReturn(response())
                whenever(valueConversionService.update(eq(1L), any())).thenThrow(duplicateTrimmedKeys())
            }

            "value-converting/delete/no-content" -> {
                whenever(valueConversionService.findById(1L)).thenReturn(response())
            }

            "value-converting/all-history/ok" -> {
                stubAuthorizedSourceApplicationIds()
                whenever(valueConversionHistoryService.findAllHistory(any(), any(), any())).thenReturn(
                    PageImpl(
                        listOf(
                            entityHistoryEntry(
                                entityId = 1L,
                                timestamp = LAST_MODIFIED_AT,
                                type = HistoryEventType.UPDATED,
                                actor = Actor.User(SECOND_ACTOR_OID),
                                actorDisplay = "Ola Nordmann",
                                snapshot = snapshot(id = 1L, displayName = "Updated display name"),
                            ),
                            entityHistoryEntry(
                                entityId = 2L,
                                timestamp = CREATED_AT,
                                type = HistoryEventType.CREATED,
                                actor = Actor.System,
                                actorDisplay = "System",
                                snapshot = snapshot(id = 2L, convertingMap = mapOf("C" to "D")),
                            ),
                        ),
                        PageRequest.of(0, 20),
                        2,
                    ),
                )
            }

            "value-converting/all-history/ok-empty" -> {
                stubAuthorizedSourceApplicationIds()
                whenever(valueConversionHistoryService.findAllHistory(any(), any(), any())).thenReturn(
                    PageImpl(
                        emptyList<EntityHistoryEntryDto<ValueConversionSnapshot, Long>>(),
                        PageRequest.of(0, 20),
                        0,
                    ),
                )
            }

            "value-converting/history-by-id/ok" -> {
                stubExistingValueConversion(id = 5L, fromApplicationId = 1L)
                whenever(valueConversionHistoryService.findHistory(eq(5L), any(), any())).thenReturn(
                    PageImpl(
                        listOf(
                            historyEntry(
                                timestamp = Instant.parse("2026-03-01T08:00:00Z"),
                                type = HistoryEventType.DELETED,
                                actor = Actor.M2M("fint-flyt-gateway"),
                                actorDisplay = null,
                                snapshot = null,
                            ),
                            historyEntry(
                                timestamp = CREATED_AT,
                                type = HistoryEventType.CREATED,
                                actor = Actor.User(FIRST_ACTOR_OID),
                                actorDisplay = "Kari Nordmann",
                                snapshot = snapshot(id = 5L),
                            ),
                        ),
                        PageRequest.of(0, 20),
                        2,
                    ),
                )
            }

            "value-converting/history-by-id/ok-time-filtered" -> {
                stubExistingValueConversion(id = 5L, fromApplicationId = 1L)
                whenever(valueConversionHistoryService.findHistory(eq(5L), any(), any())).thenReturn(
                    PageImpl(
                        listOf(
                            historyEntry(
                                timestamp = CREATED_AT,
                                type = HistoryEventType.CREATED,
                                actor = Actor.Unknown,
                                actorDisplay = null,
                                snapshot = snapshot(id = 5L),
                            ),
                        ),
                        PageRequest.of(1, 1),
                        2,
                    ),
                )
            }

            "value-converting/history-by-id/forbidden" -> {
                stubExistingValueConversion(id = 5L, fromApplicationId = 3L)
                denyAccessToSourceApplication(3L)
            }

            else -> {
                error(
                    "Fixturen '${fixture.id}' har ikke oppsett i denne testen. " +
                        "Legg det til her, ellers er kontrakten udekket i denne tjenesten.",
                )
            }
        }
    }

    /**
     * Fraværet av stubbing er tilsiktet, ikke glemt: validering eller deserialisering avviser
     * requesten før kontrolleren rører tjenestelaget. En stub her ville aldri blitt kalt, og ville
     * skjult at det er nettopp det tilfellet fastholder.
     */
    private fun requestIsRejectedBeforeServiceLayer() = Unit

    /**
     * Siden må være paged. Listeflaten svarer med hele Spring Data-formen, og feltene under
     * `pageable` leses direkte fra Pageable - på en unpaged Pageable kaster de.
     */
    private fun stubList(vararg content: ValueConversionResponse) {
        stubAuthorizedSourceApplicationIds()
        val pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Order.asc("id")))
        whenever(
            valueConversionService.findAllBySourceApplicationIds(any(), any(), any(), any()),
        ).thenReturn(PageImpl(content.toList(), pageRequest, content.size.toLong()))
    }

    private fun stubAuthorizedSourceApplicationIds() {
        whenever(valueConversionService.findDistinctSourceApplicationIds()).thenReturn(setOf(1L))
        whenever(valueConversionRepository.findDistinctSourceApplicationIds()).thenReturn(setOf(1L))
        whenever(userAuthorizationService.getUserAuthorizedSourceApplicationIds(any(), any())).thenReturn(setOf(1L))
    }

    private fun stubExistingValueConversion(
        id: Long,
        fromApplicationId: Long,
    ) = whenever(valueConversionRepository.findById(id))
        .thenReturn(Optional.of(ValueConversion(id = id, fromApplicationId = fromApplicationId)))

    private fun denyAccessToSourceApplication(sourceApplicationId: Long) {
        doThrow(
            ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access or modify data that is related to source application " +
                    "with id=$sourceApplicationId",
            ),
        ).whenever(userAuthorizationService)
            .checkIfUserHasAccessToSourceApplication(any(), eq(sourceApplicationId))
    }

    private fun duplicateTrimmedKeys() =
        ValueConversionValidationException("Validation error: convertingMap contains duplicate keys after trimming")

    private fun response(
        id: Long = 1L,
        displayName: String = "Display name",
        fromApplicationId: Long = 1L,
        fromTypeId: String = "fromType",
        toApplicationId: String = "toAppId",
        toTypeId: String = "toType",
        convertingMap: Map<String, String>? = mapOf("A" to "B"),
    ) = ValueConversionResponse(
        id = id,
        displayName = displayName,
        fromApplicationId = fromApplicationId,
        fromTypeId = fromTypeId,
        toApplicationId = toApplicationId,
        toTypeId = toTypeId,
        convertingMap = convertingMap,
        createdAt = CREATED_AT,
        createdBy = FIRST_ACTOR_OID.toString(),
        createdByActor = Actor.User(FIRST_ACTOR_OID),
        lastModifiedAt = LAST_MODIFIED_AT,
        lastModifiedBy = SECOND_ACTOR_OID.toString(),
        lastModifiedByActor = Actor.User(SECOND_ACTOR_OID),
    )

    private fun snapshot(
        id: Long,
        displayName: String = "Display name",
        convertingMap: Map<String, String> = mapOf("A" to "B"),
    ) = ValueConversionSnapshot(
        id = id,
        displayName = displayName,
        fromApplicationId = 1L,
        fromTypeId = "fromType",
        toApplicationId = "toAppId",
        toTypeId = "toType",
        convertingMap = convertingMap,
    )

    private fun historyEntry(
        timestamp: Instant,
        type: HistoryEventType,
        actor: Actor,
        actorDisplay: String?,
        snapshot: ValueConversionSnapshot?,
    ) = HistoryEntryDto(timestamp, type, actor, actorDisplay, snapshot)

    private fun entityHistoryEntry(
        entityId: Long,
        timestamp: Instant,
        type: HistoryEventType,
        actor: Actor,
        actorDisplay: String?,
        snapshot: ValueConversionSnapshot?,
    ) = EntityHistoryEntryDto(entityId, timestamp, type, actor, actorDisplay, snapshot)

    companion object {
        private val OBJECT_MAPPER = FixtureObjectMapper.springBoot()
        private val CREATED_AT: Instant = Instant.parse("2026-01-15T09:00:00Z")
        private val LAST_MODIFIED_AT: Instant = Instant.parse("2026-02-20T13:30:00Z")
        private val FIRST_ACTOR_OID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
        private val SECOND_ACTOR_OID: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")

        @JvmStatic
        fun httpContractFixtures(): List<HttpContractFixture> = CatalogContractFixtures.http("value-converting")
    }
}
