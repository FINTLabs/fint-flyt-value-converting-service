package no.fintlabs;

import no.fintlabs.model.ValueConverting;
import no.fintlabs.model.ValueConvertingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ValueConvertingMappingServiceTest {
    private ValueConvertingMappingService mappingService;

    @BeforeEach
    public void setup() {
        mappingService = new ValueConvertingMappingService();
    }

    @Test
    public void testToEntity() {
        ValueConvertingDto dto = ValueConvertingDto.builder()
                .displayName("Test Display Name")
                .fromApplicationId(1L)
                .fromTypeId("fromType")
                .toApplicationId("toAppId")
                .toTypeId("toType")
                .convertingMap(new HashMap<>())
                .build();

        ValueConverting entity = mappingService.toEntity(dto);

        assertEquals(dto.getDisplayName(), entity.getDisplayName());
        assertEquals(dto.getFromApplicationId(), entity.getFromApplicationId());
        assertEquals(dto.getFromTypeId(), entity.getFromTypeId());
        assertEquals(dto.getToApplicationId(), entity.getToApplicationId());
        assertEquals(dto.getToTypeId(), entity.getToTypeId());
        assertEquals(dto.getConvertingMap(), entity.getConvertingMap());
    }

    @Test
    public void testToDto_excludeConvertingMap() {
        Map<String, String> convertingMap = new HashMap<>();
        convertingMap.put("key1", "value1");
        convertingMap.put("key2", "value2");

        ValueConverting entity = ValueConverting.builder()
                .displayName("Test Display Name")
                .fromApplicationId(1L)
                .fromTypeId("fromType")
                .toApplicationId("toAppId")
                .toTypeId("toType")
                .convertingMap(convertingMap)
                .build();

        ValueConvertingDto dto = mappingService.toDto(entity, true);

        assertEquals(entity.getDisplayName(), dto.getDisplayName());
        assertEquals(entity.getFromApplicationId(), dto.getFromApplicationId());
        assertEquals(entity.getFromTypeId(), dto.getFromTypeId());
        assertEquals(entity.getToApplicationId(), dto.getToApplicationId());
        assertEquals(entity.getToTypeId(), dto.getToTypeId());
        assertNull(dto.getConvertingMap());
    }

    @Test
    public void testToDto_includeConvertingMap() {
        Map<String, String> convertingMap = new HashMap<>();
        convertingMap.put("key1", "value1");
        convertingMap.put("key2", "value2");

        ValueConverting entity = ValueConverting.builder()
                .displayName("Test Display Name")
                .fromApplicationId(1L)
                .fromTypeId("fromType")
                .toApplicationId("toAppId")
                .toTypeId("toType")
                .convertingMap(convertingMap)
                .build();

        ValueConvertingDto dto = mappingService.toDto(entity, false);

        assertEquals(entity.getDisplayName(), dto.getDisplayName());
        assertEquals(entity.getFromApplicationId(), dto.getFromApplicationId());
        assertEquals(entity.getFromTypeId(), dto.getFromTypeId());
        assertEquals(entity.getToApplicationId(), dto.getToApplicationId());
        assertEquals(entity.getToTypeId(), dto.getToTypeId());
        assertEquals(entity.getConvertingMap(), dto.getConvertingMap());
    }
}
