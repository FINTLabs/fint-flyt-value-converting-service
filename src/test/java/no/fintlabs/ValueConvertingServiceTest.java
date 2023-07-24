package no.fintlabs;

import no.fintlabs.model.ValueConverting;
import no.fintlabs.model.ValueConvertingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ValueConvertingServiceTest {

    @Mock
    private ValueConvertingMappingService mappingService;

    @Mock
    private ValueConvertingRepository repository;

    @InjectMocks
    private ValueConvertingService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        boolean excludeConvertingMap = true;

        ValueConverting valueConverting = ValueConverting
                .builder()
                .id(1L)
                .displayName("displayName")
                .fromApplicationId(2L)
                .fromTypeId("fromTypeId")
                .toApplicationId("toApplicationId")
                .toTypeId("toTypeId")
                .convertingMap(new HashMap<>())
                .build();

        Page<ValueConverting> valueConvertingPage = new PageImpl<>(Collections.singletonList(valueConverting), pageable, 1);

        ValueConvertingDto valueConvertingDto = ValueConvertingDto
                .builder()
                .id(valueConverting.getId())
                .displayName(valueConverting.getDisplayName())
                .fromApplicationId(valueConverting.getFromApplicationId())
                .fromTypeId(valueConverting.getFromTypeId())
                .toApplicationId(valueConverting.getToApplicationId())
                .toTypeId(valueConverting.getToTypeId())
                .convertingMap(null)
                .build();

        when(repository.findAll(pageable)).thenReturn(valueConvertingPage);
        when(mappingService.toDto(valueConverting, excludeConvertingMap)).thenReturn(valueConvertingDto);

        Page<ValueConvertingDto> actualPage = service.findAll(pageable, excludeConvertingMap);

        assertEquals(1, actualPage.getTotalElements());
        assertEquals(valueConvertingDto, actualPage.getContent().get(0));
    }

    @Test
    void findById_ExistingId_ShouldReturnOptionalOfValueConvertingDto() {
        Long valueConvertingId = 1L;
        ValueConvertingDto expectedDto = mock(ValueConvertingDto.class);
        when(repository.findById(valueConvertingId)).thenReturn(Optional.of(mock(ValueConverting.class)));

        when(mappingService.toDto(any(), anyBoolean())).thenReturn(expectedDto);

        Optional<ValueConvertingDto> result = service.findById(valueConvertingId);

        verify(repository).findById(valueConvertingId);
        verify(mappingService).toDto(any(), eq(false));
        assertEquals(Optional.of(expectedDto), result);
    }

    @Test
    void findById_NonExistingId_ShouldReturnEmptyOptional() {
        Long valueConvertingId = 1L;
        when(repository.findById(valueConvertingId)).thenReturn(Optional.empty());

        Optional<ValueConvertingDto> result = service.findById(valueConvertingId);

        verify(repository).findById(valueConvertingId);
        assertEquals(Optional.empty(), result);
    }

    @Test
    void save_ShouldReturnSavedValueConvertingDto() {
        ValueConvertingDto inputDto = mock(ValueConvertingDto.class);

        ValueConverting savedValueConverting = mock(ValueConverting.class);
        when(repository.save(any())).thenReturn(savedValueConverting);

        ValueConvertingDto expectedDto = mock(ValueConvertingDto.class);
        when(mappingService.toDto(any(), anyBoolean())).thenReturn(expectedDto);

        ValueConvertingDto result = service.save(inputDto);

        verify(mappingService).toEntity(inputDto);
        verify(repository).save(any());
        verify(mappingService).toDto(any(), eq(false));
        assertEquals(expectedDto, result);
    }
}
