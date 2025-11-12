package no.novari.value.converting;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import no.novari.flyt.resourceserver.security.user.UserAuthorizationService;
import no.novari.value.converting.model.ValueConvertingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ValueConvertingControllerTest {

    @Mock
    private ValueConvertingService valueConvertingService;

    @Mock
    private ValidationErrorsFormattingService validationErrorsFormattingService;

    @Mock
    private LocalValidatorFactoryBean validatorFactory;

    @Mock
    private UserAuthorizationService userAuthorizationService;

    @Mock
    private Validator validator;

    @Mock
    Authentication authentication;

    private PageRequest pageRequest;

    @BeforeEach
    public void setUp() {
        when(validatorFactory.getValidator()).thenReturn(validator);
        pageRequest = PageRequest.of(0, 10, Sort.Direction.ASC, "property");
    }

    private ValueConvertingController getController(boolean userPermissionsConsumerEnabled) {
        return new ValueConvertingController(
                valueConvertingService,
                validationErrorsFormattingService,
                validatorFactory,
                userAuthorizationService,
                userPermissionsConsumerEnabled
        );
    }

    @Test
    @DisplayName("returns all ValueConverting when user permissions are disabled")
    public void returnsAllValueConverting_whenUserPermissionsDisabled() {
        Page<ValueConvertingDto> mockPage = mock(Page.class);
        when(valueConvertingService.findAll(pageRequest, false)).thenReturn(mockPage);

        ResponseEntity<Page<ValueConvertingDto>> response = getController(false)
                .getValueConvertings(authentication, 0, 10, "property", Sort.Direction.ASC, false);

        verify(valueConvertingService).findAll(pageRequest, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockPage);
    }

    @Test
    @DisplayName("returns ValueConverting filtered by sourceApplicationIds when user permissions enabled")
    public void returnsValueConvertingFilteredBySourceApplicationIds_whenUserPermissionsEnabled() {
        Set<Long> mockSourceApplicationIds = Set.of(1L, 2L);
        when(userAuthorizationService.getUserAuthorizedSourceApplicationIds(authentication)).thenReturn(mockSourceApplicationIds);

        Page<ValueConvertingDto> mockPage = mock(Page.class);
        when(valueConvertingService.findAllBySourceApplicationIds(pageRequest, false, mockSourceApplicationIds))
                .thenReturn(mockPage);

        ResponseEntity<Page<ValueConvertingDto>> response = getController(true)
                .getValueConvertings(authentication, 0, 10, "property", Sort.Direction.ASC, false);

        verify(userAuthorizationService).getUserAuthorizedSourceApplicationIds(authentication);
        verify(valueConvertingService).findAllBySourceApplicationIds(pageRequest, false, mockSourceApplicationIds);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockPage);
    }

    @Test
    @DisplayName("returns ValueConverting with HTTP 200 if found")
    public void returnsValueConvertingWithHttp200_ifFound() {
        ValueConvertingDto dto = ValueConvertingDto.builder().build();
        when(valueConvertingService.findById(1L)).thenReturn(Optional.of(dto));

        ResponseEntity<ValueConvertingDto> response = getController(false)
                .getValueConverting(authentication, 1L);

        verify(valueConvertingService).findById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("throws Forbidden exception if user does not have access to ValueConverting")
    public void throwsForbiddenException_ifUserHasNoAccessToValueConverting() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
                .when(userAuthorizationService)
                .checkIfUserHasAccessToSourceApplication(authentication, 1L);

        ValueConvertingDto valueConvertingDto = mock(ValueConvertingDto.class);
        when(valueConvertingDto.getFromApplicationId()).thenReturn(1L);

        when(valueConvertingService.findById(1L)).thenReturn(Optional.of(valueConvertingDto));

        assertThatThrownBy(() -> getController(true).getValueConverting(authentication, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN)
                .hasMessageContaining("Forbidden");

        verify(userAuthorizationService).checkIfUserHasAccessToSourceApplication(authentication, 1L);
        verify(valueConvertingService).findById(1L);
    }

    @Test
    @DisplayName("throws NotFound exception when ValueConverting is not found")
    public void throwsNotFoundException_whenValueConvertingNotFound() {
        when(valueConvertingService.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getController(false).getValueConverting(authentication, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);

        verify(valueConvertingService).findById(1L);
    }

    @Test
    @DisplayName("throws exception when posting ValueConverting with validation errors")
    public void throwsException_whenPostingWithValidationErrors() {
        ValueConvertingDto dto = ValueConvertingDto.builder().build();
        Set<ConstraintViolation<ValueConvertingDto>> violations = Set.of(mock(ConstraintViolation.class));
        when(validatorFactory.getValidator().validate(dto)).thenReturn(violations);
        when(validationErrorsFormattingService.format(violations)).thenReturn("Error message");

        assertThatThrownBy(() -> getController(false).postValueConverting(authentication, dto))
                .isInstanceOf(ResponseStatusException.class);

        verify(validatorFactory.getValidator()).validate(dto);
        verify(validationErrorsFormattingService).format(violations);
    }

    @Test
    @DisplayName("throws Forbidden exception if user does not have access to ValueConverting on POST")
    public void throwsForbiddenException_ifUserHasNoAccessOnPost() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
                .when(userAuthorizationService)
                .checkIfUserHasAccessToSourceApplication(any(), any());

        ValueConvertingDto valueConvertingDto = mock(ValueConvertingDto.class);
        when(valueConvertingDto.getFromApplicationId()).thenReturn(1L);

        assertThatThrownBy(() -> getController(true).postValueConverting(authentication, valueConvertingDto))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN)
                .hasMessageContaining("Forbidden");

        verify(userAuthorizationService).checkIfUserHasAccessToSourceApplication(any(), any());
    }

    @Test
    @DisplayName("posts ValueConverting with no validation errors and user permissions disabled")
    public void postsValueConvertingSuccessfully_whenNoErrorsAndUserPermissionsDisabled() {
        ValueConvertingDto dto = ValueConvertingDto.builder().build();
        when(validator.validate(dto)).thenReturn(Collections.emptySet());
        when(valueConvertingService.save(dto)).thenReturn(dto);

        ResponseEntity<ValueConvertingDto> response = getController(false)
                .postValueConverting(authentication, dto);

        verify(validator).validate(dto);
        verify(valueConvertingService).save(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("posts ValueConverting with no validation errors and user permissions enabled with access")
    public void postsValueConvertingSuccessfully_whenNoErrorsAndUserPermissionsEnabledWithAccess() {
        ValueConvertingDto valueConvertingDto = mock(ValueConvertingDto.class);
        when(valueConvertingDto.getFromApplicationId()).thenReturn(2L);

        when(validator.validate(valueConvertingDto)).thenReturn(Collections.emptySet());
        when(valueConvertingService.save(valueConvertingDto)).thenReturn(valueConvertingDto);

        ResponseEntity<ValueConvertingDto> response = getController(true)
                .postValueConverting(authentication, valueConvertingDto);

        verify(validator).validate(valueConvertingDto);
        verify(valueConvertingService).save(valueConvertingDto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(valueConvertingDto);
    }
}
