package no.novari.value.converting.application

import no.novari.value.converting.domain.ValueConversion
import no.novari.value.converting.domain.ValueConversionMapper
import no.novari.value.converting.infrastructure.persistence.ValueConversionRepository
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
    ValueConversionDeletionAuditIntegrationTest.AuditTestConfiguration::class,
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ValueConversionDeletionAuditIntegrationTest {
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
