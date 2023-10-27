package no.fintlabs;

import no.fintlabs.model.ValueConverting;
import no.fintlabs.model.ValueConvertingDto;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ValueConvertingMappingService {
    public ValueConverting toEntity(ValueConvertingDto valueConvertingDto) {

        Map<String, String> trimmedMap = valueConvertingDto.getConvertingMap().entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().trim(),
                        entry -> entry.getValue().trim()
                ));

        return ValueConverting
                .builder()
                .displayName(valueConvertingDto.getDisplayName())
                .fromApplicationId(valueConvertingDto.getFromApplicationId())
                .fromTypeId(valueConvertingDto.getFromTypeId())
                .toApplicationId(valueConvertingDto.getToApplicationId())
                .toTypeId(valueConvertingDto.getToTypeId())
                .convertingMap(trimmedMap)
                .build();
    }

    public ValueConvertingDto toDto(ValueConverting valueConverting, boolean excludeConvertingMap) {
        return ValueConvertingDto
                .builder()
                .id(valueConverting.getId())
                .displayName(valueConverting.getDisplayName())
                .fromApplicationId(valueConverting.getFromApplicationId())
                .fromTypeId(valueConverting.getFromTypeId())
                .toApplicationId(valueConverting.getToApplicationId())
                .toTypeId(valueConverting.getToTypeId())
                .convertingMap(excludeConvertingMap
                        ? null
                        : new HashMap<>(valueConverting.getConvertingMap())
                )
                .build();
    }
}
