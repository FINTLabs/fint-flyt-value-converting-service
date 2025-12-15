package no.novari.value.converting;

import no.novari.value.converting.model.ValueConvertingDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class ValueConvertingService {

    private final ValueConvertingMappingService valueConvertingMappingService;
    private final ValueConvertingRepository valueConvertingRepository;


    public ValueConvertingService(ValueConvertingMappingService valueConvertingMappingService, ValueConvertingRepository valueConvertingRepository) {
        this.valueConvertingMappingService = valueConvertingMappingService;
        this.valueConvertingRepository = valueConvertingRepository;
    }

    public Page<ValueConvertingDto> findAll(Pageable pageable, boolean excludeConvertingMap) {
        return valueConvertingRepository
                .findAll(pageable)
                .map(valueConverting -> valueConvertingMappingService.toDto(
                        valueConverting,
                        excludeConvertingMap
                ));
    }

    public Page<ValueConvertingDto> findAllBySourceApplicationIds(
            Pageable pageable,
            boolean excludeConvertingMap,
            Set<Long> sourceApplicationIds
    ) {
        return valueConvertingRepository
                .findAllByFromApplicationIdIn(pageable, sourceApplicationIds)
                .map(valueConverting -> valueConvertingMappingService.toDto(
                        valueConverting,
                        excludeConvertingMap
                ));
    }

    public Optional<ValueConvertingDto> findById(Long valueConvertingId) {
        return valueConvertingRepository
                .findById(valueConvertingId)
                .map(valueConverting -> valueConvertingMappingService.toDto(
                        valueConverting,
                        false
                ));
    }

    public ValueConvertingDto save(ValueConvertingDto valueConvertingDto) {
        return valueConvertingMappingService
                .toDto(
                        valueConvertingRepository.save(
                                valueConvertingMappingService.toEntity(valueConvertingDto)
                        ),
                        false
                );
    }

}
