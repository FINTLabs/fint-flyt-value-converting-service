package no.novari.flyt.value.converting.application

import no.novari.flyt.value.converting.api.dto.ValueConversionRequest
import no.novari.flyt.value.converting.domain.ValueConversion
import no.novari.flyt.value.converting.domain.ValueConversionMapper
import no.novari.flyt.value.converting.infrastructure.persistence.ValueConversionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(
    properties = [
        "spring.profiles.include=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/",
        "spring.datasource.hikari.schema=public",
    ],
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    ValueConversionService::class,
    ValueConversionMapper::class,
    ValueConversionAuditIntegrationTest.AuditTestConfiguration::class,
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ValueConversionAuditIntegrationTest {
    @Autowired
    private lateinit var valueConversionRepository: ValueConversionRepository

    @Autowired
    private lateinit var valueConversionService: ValueConversionService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `updating value conversion should replace converting map and write update audit revisions`() {
        val actorId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        setAuthenticatedUser(actorId)

        val valueConversion =
            valueConversionRepository.saveAndFlush(
                ValueConversion(
                    displayName = "Display name",
                    fromApplicationId = 42L,
                    fromTypeId = "fromType",
                    toApplicationId = "toApp",
                    toTypeId = "toType",
                    convertingMap = mutableMapOf("A" to "B", "C" to "D"),
                ),
            )
        val valueConversionId = checkNotNull(valueConversion.id)

        val response =
            valueConversionService.update(
                valueConversionId,
                ValueConversionRequest(
                    displayName = "Updated display name",
                    fromApplicationId = 43L,
                    fromTypeId = "updatedFromType",
                    toApplicationId = "updatedToApp",
                    toTypeId = "updatedToType",
                    convertingMap = mapOf(" A " to " Updated ", "E" to " F "),
                ),
            )

        assertThat(response.id).isEqualTo(valueConversionId)
        assertThat(response.convertingMap)
            .containsExactlyInAnyOrderEntriesOf(mapOf("A" to "Updated", "E" to "F"))

        val currentConvertingMapRows =
            jdbcTemplate.queryForList(
                """
                select key, value
                from converting_map
                where value_converting_id = ?
                order by key
                """.trimIndent(),
                valueConversionId,
            )
        assertThat(currentConvertingMapRows.associate { it["key"] as String to it["value"] as String })
            .containsExactlyInAnyOrderEntriesOf(mapOf("A" to "Updated", "E" to "F"))

        val valueConversionAuditRows =
            jdbcTemplate.queryForList(
                """
                select rev, revtype, display_name, from_application_id
                from value_converting_aud
                where id = ?
                order by rev
                """.trimIndent(),
                valueConversionId,
            )

        assertThat(valueConversionAuditRows.map { (it["revtype"] as Number).toInt() })
            .containsExactly(0, 1)

        val updateAuditRow = valueConversionAuditRows.last()
        val updateRevision = (updateAuditRow["rev"] as Number).toLong()
        assertThat(updateAuditRow["display_name"]).isEqualTo("Updated display name")
        assertThat((updateAuditRow["from_application_id"] as Number).toLong()).isEqualTo(43L)

        val convertingMapUpdateRows =
            jdbcTemplate.queryForList(
                """
                select revtype, key, value
                from converting_map_aud
                where value_converting_id = ? and rev = ?
                order by key
                """.trimIndent(),
                valueConversionId,
                updateRevision,
            )

        val convertingMapUpdateRowsByKeyAndRevisionType =
            convertingMapUpdateRows.associateBy {
                it["key"] as String to (it["revtype"] as Number).toInt()
            }
        assertThat(convertingMapUpdateRowsByKeyAndRevisionType.keys)
            .containsExactlyInAnyOrder(
                "A" to 0,
                "A" to 2,
                "C" to 2,
                "E" to 0,
            )
        assertThat(convertingMapUpdateRowsByKeyAndRevisionType.getValue("A" to 0)["value"]).isEqualTo("Updated")
        assertThat(convertingMapUpdateRowsByKeyAndRevisionType.getValue("A" to 2)["value"]).isEqualTo("B")
        assertThat(convertingMapUpdateRowsByKeyAndRevisionType.getValue("C" to 2)["value"]).isEqualTo("D")
        assertThat(convertingMapUpdateRowsByKeyAndRevisionType.getValue("E" to 0)["value"]).isEqualTo("F")

        val updateRevisionActor =
            jdbcTemplate.queryForObject(
                "select actor::text from revinfo where rev = ?",
                String::class.java,
                updateRevision,
            )

        assertThat(updateRevisionActor)
            .contains("USER")
            .contains(actorId.toString())
    }

    @Test
    fun `deleting value conversion should remove converting map rows and write delete audit revisions`() {
        val actorId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        setAuthenticatedUser(actorId)

        val valueConversion =
            valueConversionRepository.saveAndFlush(
                ValueConversion(
                    displayName = "Display name",
                    fromApplicationId = 42L,
                    fromTypeId = "fromType",
                    toApplicationId = "toApp",
                    toTypeId = "toType",
                    convertingMap = mutableMapOf("A" to "B", "C" to "D"),
                ),
            )
        val valueConversionId = checkNotNull(valueConversion.id)

        valueConversionService.delete(valueConversionId)

        assertThat(valueConversionRepository.existsById(valueConversionId)).isFalse()
        assertThat(valueConversionService.findById(valueConversionId)).isNull()
        assertThat(
            countRows(
                "select count(*) from converting_map where value_converting_id = ?",
                valueConversionId,
            ),
        ).isZero()

        val valueConversionAuditRows =
            jdbcTemplate.queryForList(
                """
                select rev, revtype, display_name, from_application_id
                from value_converting_aud
                where id = ?
                order by rev
                """.trimIndent(),
                valueConversionId,
            )

        assertThat(valueConversionAuditRows.map { (it["revtype"] as Number).toInt() })
            .containsExactly(0, 2)

        val deleteAuditRow = valueConversionAuditRows.last()
        val deleteRevision = (deleteAuditRow["rev"] as Number).toLong()
        assertThat(deleteAuditRow["display_name"]).isEqualTo("Display name")
        assertThat((deleteAuditRow["from_application_id"] as Number).toLong()).isEqualTo(42L)

        val convertingMapDeleteRows =
            jdbcTemplate.queryForList(
                """
                select revtype, key, value
                from converting_map_aud
                where value_converting_id = ? and rev = ?
                order by key
                """.trimIndent(),
                valueConversionId,
                deleteRevision,
            )

        assertThat(convertingMapDeleteRows).hasSize(2)
        assertThat(convertingMapDeleteRows.map { (it["revtype"] as Number).toInt() })
            .containsOnly(2)
        assertThat(convertingMapDeleteRows.associate { it["key"] as String to it["value"] as String })
            .containsExactlyInAnyOrderEntriesOf(mapOf("A" to "B", "C" to "D"))

        val deleteRevisionActor =
            jdbcTemplate.queryForObject(
                "select actor::text from revinfo where rev = ?",
                String::class.java,
                deleteRevision,
            )

        assertThat(deleteRevisionActor)
            .contains("USER")
            .contains(actorId.toString())
    }

    private fun countRows(
        sql: String,
        vararg args: Any,
    ): Long = jdbcTemplate.queryForObject(sql, Long::class.java, *args) ?: 0L

    private fun setAuthenticatedUser(actorId: UUID) {
        val jwt =
            Jwt
                .withTokenValue("test-token")
                .header("alg", "none")
                .claim("objectidentifier", actorId.toString())
                .build()
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = TestingAuthenticationToken(jwt, "credentials", "ROLE_TEST")
        SecurityContextHolder.setContext(context)
    }

    @TestConfiguration
    @EnableJpaAuditing(auditorAwareRef = "flytAuditorAware")
    class AuditTestConfiguration

    companion object {
        @Container
        @JvmField
        val postgres: PostgreSQLContainer<Nothing> = PostgreSQLContainer("postgres:16-alpine")

        @JvmStatic
        @DynamicPropertySource
        fun databaseProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.hikari.schema") { "public" }
            registry.add("fint.database.url", postgres::getJdbcUrl)
            registry.add("fint.database.username", postgres::getUsername)
            registry.add("fint.database.password", postgres::getPassword)
        }
    }
}
