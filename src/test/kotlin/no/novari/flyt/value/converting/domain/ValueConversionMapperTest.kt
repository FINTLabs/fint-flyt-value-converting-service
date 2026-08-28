package no.novari.flyt.value.converting.domain

import no.novari.flyt.value.converting.api.dto.ValueConversionRequest
import no.novari.flyt.value.converting.api.exception.ValueConversionValidationException
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
    fun `updating entity from request should keep id and replace converting map`() {
        val entity =
            ValueConversion(
                id = 7L,
                displayName = "Old Display Name",
                fromApplicationId = 1L,
                fromTypeId = "oldFromType",
                toApplicationId = "oldToAppId",
                toTypeId = "oldToType",
                convertingMap = mutableMapOf("oldKey" to "oldValue", "removedKey" to "removedValue"),
            )
        val request =
            ValueConversionRequest(
                displayName = "Updated Display Name",
                fromApplicationId = 2L,
                fromTypeId = "updatedFromType",
                toApplicationId = "updatedToAppId",
                toTypeId = "updatedToType",
                convertingMap = mapOf(" newKey " to " newValue "),
            )

        val updatedEntity = mapper.updateEntity(entity, request)

        assertEquals(7L, updatedEntity.id)
        assertEquals(request.displayName, updatedEntity.displayName)
        assertEquals(request.fromApplicationId, updatedEntity.fromApplicationId)
        assertEquals(request.fromTypeId, updatedEntity.fromTypeId)
        assertEquals(request.toApplicationId, updatedEntity.toApplicationId)
        assertEquals(request.toTypeId, updatedEntity.toTypeId)
        assertEquals(mapOf("newKey" to "newValue"), updatedEntity.convertingMap)
    }

    @Test
    fun `updating entity from request should throw when trimmed map keys collide`() {
        val entity =
            ValueConversion(
                id = 7L,
                displayName = "Old Display Name",
                fromApplicationId = 1L,
                fromTypeId = "oldFromType",
                toApplicationId = "oldToAppId",
                toTypeId = "oldToType",
                convertingMap = mutableMapOf("oldKey" to "oldValue"),
            )
        val request =
            ValueConversionRequest(
                displayName = "Updated Display Name",
                fromApplicationId = 2L,
                fromTypeId = "updatedFromType",
                toApplicationId = "updatedToAppId",
                toTypeId = "updatedToType",
                convertingMap =
                    mapOf(
                        "Key " to "value1",
                        " Key" to "value2",
                    ),
            )

        val exception =
            assertThrows(ValueConversionValidationException::class.java) {
                mapper.updateEntity(entity, request)
            }

        assertEquals(
            "Validation error: convertingMap contains duplicate keys after trimming",
            exception.message,
        )
        assertEquals("Old Display Name", entity.displayName)
        assertEquals(1L, entity.fromApplicationId)
        assertEquals("oldFromType", entity.fromTypeId)
        assertEquals("oldToAppId", entity.toApplicationId)
        assertEquals("oldToType", entity.toTypeId)
        assertEquals(mapOf("oldKey" to "oldValue"), entity.convertingMap)
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

    @Test
    fun `mapping entity to snapshot should map business fields including converting map`() {
        val convertingMap = hashMapOf("key1" to "value1")
        val entity =
            ValueConversion(
                id = 7L,
                displayName = "Test Display Name",
                fromApplicationId = 1L,
                fromTypeId = "fromType",
                toApplicationId = "toAppId",
                toTypeId = "toType",
                convertingMap = convertingMap,
            )

        val snapshot = mapper.toSnapshot(entity)

        assertEquals(7L, snapshot.id)
        assertEquals(entity.displayName, snapshot.displayName)
        assertEquals(entity.fromApplicationId, snapshot.fromApplicationId)
        assertEquals(entity.fromTypeId, snapshot.fromTypeId)
        assertEquals(entity.toApplicationId, snapshot.toApplicationId)
        assertEquals(entity.toTypeId, snapshot.toTypeId)
        assertEquals(convertingMap, snapshot.convertingMap)
    }
}
