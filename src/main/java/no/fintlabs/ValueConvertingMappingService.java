package no.fintlabs;

import no.fintlabs.model.ValueConverting;
import no.fintlabs.model.ValueConvertingDto;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class ValueConvertingMappingService {
    public ValueConverting toEntity(ValueConvertingDto valueConvertingDto) {
        return ValueConverting
                .builder()
                .fromApplicationId(valueConvertingDto.getFromApplicationId())
                .fromTypeId(valueConvertingDto.getFromTypeId())
                .toApplicationId(valueConvertingDto.getToApplicationId())
                .toTypeId(valueConvertingDto.getToTypeId())
                .convertingMap(valueConvertingDto.getConvertingMap())
                .build();
    }

    public ValueConvertingDto toDto(ValueConverting valueConverting, boolean excludeConvertingMap) {
        return ValueConvertingDto
                .builder()
                .id(valueConverting.getId())
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
