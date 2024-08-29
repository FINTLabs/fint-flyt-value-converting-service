package no.fintlabs;

import no.fintlabs.model.ValueConvertingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class ValueConvertingControllerTest {

    @InjectMocks
    private ValueConvertingController controller;

    @Mock
    private ValueConvertingService valueConvertingService;

    @Mock
    private ValidationErrorsFormattingService validationErrorsFormattingService;

    @Mock
    private LocalValidatorFactoryBean validatorFactory;

    @Mock
    private Validator validator;

    @Mock
    Authentication authentication;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(validatorFactory.getValidator()).thenReturn(validator);

        controller = new ValueConvertingController(valueConvertingService, validationErrorsFormattingService, validatorFactory);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnAllValueConvertingsWithUserPermissionsDisabled() {
        Page<ValueConvertingDto> mockPage = mock(Page.class);
        when(valueConvertingService.findAll(any(PageRequest.class), anyBoolean())).thenReturn(mockPage);

        ResponseEntity<Page<ValueConvertingDto>> response = controller.getValueConvertings(authentication, 0, 10, "property", Sort.Direction.ASC, false);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(mockPage, response.getBody());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnSpecificValueConvertingsWithUserPermissionsEnabled() throws NoSuchFieldException, IllegalAccessException {
        setUserPermissionsConsumerEnabled();

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("sourceApplicationIds")).thenReturn("1,2");

        when(authentication.getPrincipal()).thenReturn(jwt);

        List<Long> mockSourceApplicationIds = List.of(1L, 2L);

        Page<ValueConvertingDto> mockPage = mock(Page.class);
        when(valueConvertingService.findAllBySourceApplicationIds(
                any(PageRequest.class), anyBoolean(), eq(mockSourceApplicationIds)
        )).thenReturn(mockPage);

        ResponseEntity<Page<ValueConvertingDto>> response = controller.getValueConvertings(
                authentication, 0, 10, "property", Sort.Direction.ASC, false
        );

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(mockPage, response.getBody());
    }

    @Test
    public void shouldReturnHttp200IfGetValueConvertingFound() {
        ValueConvertingDto dto = ValueConvertingDto.builder().build();
        when(valueConvertingService.findById(1L)).thenReturn(Optional.of(dto));

        ResponseEntity<ValueConvertingDto> response = controller.getValueConverting(authentication, 1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dto, response.getBody());
    }

    @Test
    public void shouldThrowExceptionForbiddenIfUserDontHaveAccessToValueConverting() throws NoSuchFieldException, IllegalAccessException {
        setUserPermissionsConsumerEnabled();

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("sourceApplicationIds")).thenReturn("2");

        when(authentication.getPrincipal()).thenReturn(jwt);

        ValueConvertingDto valueConvertingDto = mock(ValueConvertingDto.class);
        when(valueConvertingDto.getFromApplicationId()).thenReturn(1L);

        when(valueConvertingService.findById(1L)).thenReturn(Optional.of(valueConvertingDto));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getValueConverting(authentication, 1L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("You do not have permission to access or modify data that is related to source application with id=1", exception.getReason());
    }


    @Test
    public void shouldThrowExceptionWhenGetValueConvertingIsNotFound() {
        when(valueConvertingService.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getValueConverting(authentication, 1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldThrowExceptionWhenPostingWithValidationErrors() {
        ValueConvertingDto dto = ValueConvertingDto.builder().build();
        Set<ConstraintViolation<ValueConvertingDto>> violations = Collections.singleton(mock(ConstraintViolation.class));
        when(validatorFactory.getValidator().validate(dto)).thenReturn(violations);
        when(validationErrorsFormattingService.format(violations)).thenReturn("Error message");

        assertThrows(ResponseStatusException.class, () -> controller.postValueConverting(authentication, dto));
    }

    @Test
    public void shouldThrowExceptionForbiddenIfUserDontHaveAccessToValueConvertingOnPost() throws NoSuchFieldException, IllegalAccessException {
        setUserPermissionsConsumerEnabled();

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("sourceApplicationIds")).thenReturn("2");
        when(authentication.getPrincipal()).thenReturn(jwt);

        ValueConvertingDto valueConvertingDto = mock(ValueConvertingDto.class);
        when(valueConvertingDto.getFromApplicationId()).thenReturn(1L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.postValueConverting(authentication, valueConvertingDto)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("You do not have permission to access or modify data that is related to source application with id=1", exception.getReason());
    }

    @Test
    public void shouldPostValueConvertingWithNoErrors() {
        ValueConvertingDto dto = ValueConvertingDto.builder().build();
        when(validator.validate(dto)).thenReturn(Collections.emptySet());
        when(valueConvertingService.save(dto)).thenReturn(dto);

        ResponseEntity<ValueConvertingDto> response = controller.postValueConverting(authentication, dto);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(dto, response.getBody());
    }

    @Test
    public void shouldPostValueConvertingWithNoErrorsWhenUserPermissionEnabledAndUserHasAccessToValueConverting() throws NoSuchFieldException, IllegalAccessException {
        setUserPermissionsConsumerEnabled();

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("sourceApplicationIds")).thenReturn("2");
        when(authentication.getPrincipal()).thenReturn(jwt);

        ValueConvertingDto valueConvertingDto = mock(ValueConvertingDto.class);
        when(valueConvertingDto.getFromApplicationId()).thenReturn(2L);

        when(validator.validate(valueConvertingDto)).thenReturn(Collections.emptySet());
        when(valueConvertingService.save(valueConvertingDto)).thenReturn(valueConvertingDto);

        ResponseEntity<ValueConvertingDto> response = controller.postValueConverting(authentication, valueConvertingDto);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(valueConvertingDto, response.getBody());
    }

    private void setUserPermissionsConsumerEnabled() throws NoSuchFieldException, IllegalAccessException {
        java.lang.reflect.Field field = ValueConvertingController.class.getDeclaredField("userPermissionsConsumerEnabled");
        field.setAccessible(true);
        field.set(controller, true);
    }


}
