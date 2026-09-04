package no.novari.flyt.value.converting.infrastructure.messaging

import no.novari.flyt.audit.actor.Actor
import no.novari.flyt.catalog.contract.fixtures.CatalogContractFixtures
import no.novari.flyt.catalog.contract.fixtures.KafkaPayloadFixtureRunner
import no.novari.flyt.value.converting.domain.ValueConversion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Fastholder payloadene på `request.value-converting.by.value-converting-id`, som må bestå etter
 * sammenslåingen fordi mapping-service er klient. Hver rolle testes i den retningen tjenesten
 * faktisk bruker den: requesten deserialiseres, svaret serialiseres.
 */
class ValueConversionKafkaContractTest {
    private val runner = KafkaPayloadFixtureRunner()

    @Test
    fun `requesten er en bar Long`() {
        val fixture = CatalogContractFixtures.kafkaById("value-converting/request/value-converting-by-id")

        val valueConversionId = runner.deserialize<Long>(fixture)

        assertThat(valueConversionId).isEqualTo(1L)
    }

    @Test
    fun `svaret er entiteten med audit-feltene og uten id`() {
        val fixture = CatalogContractFixtures.kafkaById("value-converting/reply/value-converting-by-id")

        runner.verifySerialization(fixture, storedValueConversion())
    }

    @Test
    fun `ukjent id gir tom payload`() {
        val fixture = CatalogContractFixtures.kafkaById("value-converting/reply/value-converting-by-id-not-found")

        runner.verifySerialization(fixture, null)
    }

    private fun storedValueConversion() =
        ValueConversion(
            id = 1L,
            displayName = "Display name",
            fromApplicationId = 1L,
            fromTypeId = "fromType",
            toApplicationId = "toAppId",
            toTypeId = "toType",
            convertingMap = mutableMapOf("A" to "B"),
        ).withAuditFieldsAsIfLoadedFromDatabase()

    /**
     * Audit-feltene har `protected set` og populeres av JPA-auditing, ikke av konstruktøren. Denne
     * testen kjører uten database, så feltene settes direkte — ellers ville payloaden hatt null der
     * en rad hentet fra databasen har verdier, og fixturen ville fastholdt feil form.
     */
    private fun ValueConversion.withAuditFieldsAsIfLoadedFromDatabase() =
        apply {
            setInheritedField("createdAt", Instant.parse("2026-01-15T09:00:00Z"))
            setInheritedField("createdBy", Actor.User(UUID.fromString("11111111-1111-1111-1111-111111111111")))
            setInheritedField("lastModifiedAt", Instant.parse("2026-02-20T13:30:00Z"))
            setInheritedField("lastModifiedBy", Actor.User(UUID.fromString("22222222-2222-2222-2222-222222222222")))
        }

    private fun Any.setInheritedField(
        name: String,
        value: Any?,
    ) {
        generateSequence(javaClass) { it.superclass }
            .mapNotNull { type -> runCatching { type.getDeclaredField(name) }.getOrNull() }
            .firstOrNull()
            ?.also { field ->
                field.isAccessible = true
                field.set(this, value)
            }
            ?: error("Fant ikke feltet '$name' på ${javaClass.name} eller superklassene")
    }
}
