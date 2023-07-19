package no.fintlabs;

import no.fintlabs.model.ValueConverting;
import no.fintlabs.model.ValueConvertingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
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
    @Disabled
    void findAll_ShouldReturnPageOfValueConvertingDto() {
        Pageable pageable = mock(Pageable.class);
        Page<ValueConverting> expectedPage = new PageImpl<>(Collections.emptyList());

        when(repository.findAll(pageable)).thenReturn(expectedPage);

        ValueConvertingDto dto = mock(ValueConvertingDto.class);

        when(mappingService.toDto(any(), anyBoolean())).thenReturn(dto);

        Page<ValueConvertingDto> result = service.findAll(pageable, false);

        verify(repository).findAll(pageable);
        verify(mappingService).toDto(any(), eq(false));

        assertEquals(expectedPage, result);
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
