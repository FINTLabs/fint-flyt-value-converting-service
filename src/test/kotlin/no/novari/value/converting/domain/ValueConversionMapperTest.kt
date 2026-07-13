package no.novari.value.converting.domain

import no.novari.value.converting.api.dto.ValueConversionRequest
import no.novari.value.converting.api.exception.ValueConversionValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ValueConversionMapperTest {
    private lateinit var mapper: ValueConversionMapper

    @BeforeEach
    fun setup() {
        mapper = ValueConversionMapper()
    }

    @Test
    fun `mapping request to entity should map all fields`() {
        val request =
            ValueConversionRequest(
                displayName = "Test Display Name",
                fromApplicationId = 1L,
                fromTypeId = "fromType",
                toApplicationId = "toAppId",
                toTypeId = "toType",
                convertingMap = hashMapOf(),
            )

        val entity = mapper.toEntity(request)

        assertEquals(request.displayName, entity.displayName)
        assertEquals(request.fromApplicationId, entity.fromApplicationId)
        assertEquals(request.fromTypeId, entity.fromTypeId)
        assertEquals(request.toApplicationId, entity.toApplicationId)
        assertEquals(request.toTypeId, entity.toTypeId)
        assertEquals(request.convertingMap, entity.convertingMap)
    }

    @Test
    fun `mapping request to entity should trim strings in converting map`() {
        val convertingMap =
            hashMapOf(
                " key1 " to " value1 ",
                " key2 " to " value2 ",
            )

        val request =
            ValueConversionRequest(
                displayName = "Test Display Name",
                fromApplicationId = 1L,
                fromTypeId = "fromType",
                toApplicationId = "toAppId",
                toTypeId = "toType",
                convertingMap = convertingMap,
            )

        val entity = mapper.toEntity(request)

        convertingMap.forEach { (key, value) ->
            val expectedTrimmedKey = key.trim()
            val expectedTrimmedValue = value.trim()

            assertTrue(entity.convertingMap.containsKey(expectedTrimmedKey))
            assertEquals(expectedTrimmedValue, entity.convertingMap[expectedTrimmedKey])
        }
    }

    @Test
    fun `mapping request to entity should throw when trimmed map keys collide`() {
        val request =
            ValueConversionRequest(
                displayName = "Test Display Name",
                fromApplicationId = 1L,
                fromTypeId = "fromType",
                toApplicationId = "toAppId",
                toTypeId = "toType",
                convertingMap =
                    mapOf(
                        "Key " to "value1",
                        " Key" to "value2",
                    ),
            )

        val exception =
            assertThrows(ValueConversionValidationException::class.java) {
                mapper.toEntity(request)
            }

        assertEquals(
            "Validation error: convertingMap contains duplicate keys after trimming",
            exception.message,
        )
    }

    @Test
    fun `mapping entity to response should exclude converting map when requested`() {
        val convertingMap =
            hashMapOf(
                "key1" to "value1",
                "key2" to "value2",
            )

        val entity =
            ValueConversion(
                displayName = "Test Display Name",
                fromApplicationId = 1L,
                fromTypeId = "fromType",
                toApplicationId = "toAppId",
                toTypeId = "toType",
                convertingMap = convertingMap,
            )

        val response = mapper.toResponse(entity, false)

        assertEquals(entity.displayName, response.displayName)
        assertEquals(entity.fromApplicationId, response.fromApplicationId)
        assertEquals(entity.fromTypeId, response.fromTypeId)
        assertEquals(entity.toApplicationId, response.toApplicationId)
        assertEquals(entity.toTypeId, response.toTypeId)
        assertNull(response.convertingMap)
    }

    @Test
    fun `mapping entity to response should include converting map when requested`() {
        val convertingMap =
            hashMapOf(
                "key1" to "value1",
                "key2" to "value2",
            )

        val entity =
            ValueConversion(
                displayName = "Test Display Name",
                fromApplicationId = 1L,
                fromTypeId = "fromType",
                toApplicationId = "toAppId",
                toTypeId = "toType",
                convertingMap = convertingMap,
            )

        val response = mapper.toResponse(entity, true)

        assertEquals(entity.displayName, response.displayName)
        assertEquals(entity.fromApplicationId, response.fromApplicationId)
        assertEquals(entity.fromTypeId, response.fromTypeId)
        assertEquals(entity.toApplicationId, response.toApplicationId)
        assertEquals(entity.toTypeId, response.toTypeId)
        assertEquals(entity.convertingMap, response.convertingMap)
    }

    @Test
    fun `mapping entity to response should include hydrated audit fields`() {
        val entity =
            ValueConversion(
                displayName = "Test Display Name",
                fromApplicationId = 1L,
                fromTypeId = "fromType",
                toApplicationId = "toAppId",
                toTypeId = "toType",
                convertingMap = hashMapOf(),
            )

        val response = mapper.toResponse(entity, false, "Ola Nordmann", "System")

        assertEquals(entity.createdAt, response.createdAt)
        assertEquals("Ola Nordmann", response.createdBy)
        assertEquals(entity.createdBy, response.createdByActor)
        assertEquals(entity.lastModifiedAt, response.lastModifiedAt)
        assertEquals("System", response.lastModifiedBy)
        assertEquals(entity.lastModifiedBy, response.lastModifiedByActor)
    }

    @Test
    fun `mapping entity to response should default audit display names to null`() {
        val entity =
            ValueConversion(
                displayName = "Test Display Name",
                fromApplicationId = 1L,
                fromTypeId = "fromType",
                toApplicationId = "toAppId",
                toTypeId = "toType",
                convertingMap = hashMapOf(),
            )

        val response = mapper.toResponse(entity, false)

        assertNull(response.createdBy)
        assertNull(response.lastModifiedBy)
    }
}
