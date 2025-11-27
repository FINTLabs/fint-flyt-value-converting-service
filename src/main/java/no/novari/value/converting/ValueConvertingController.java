package no.novari.value.converting;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import no.novari.flyt.resourceserver.security.user.UserAuthorizationService;
import no.novari.value.converting.model.ValueConvertingDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

import static no.novari.flyt.resourceserver.UrlPaths.INTERNAL_API;

@RestController
@RequestMapping(INTERNAL_API + "/value-convertings")
public class ValueConvertingController {

    private final ValueConvertingService valueConvertingService;
    private final ValidationErrorsFormattingService validationErrorsFormattingService;
    private final Validator validator;
    private final UserAuthorizationService userAuthorizationService;

    public ValueConvertingController(
            ValueConvertingService valueConvertingService,
            ValidationErrorsFormattingService validationErrorsFormattingService,
            ValidatorFactory validatorFactory,
            UserAuthorizationService userAuthorizationService
    ) {
        this.validator = validatorFactory.getValidator();
        this.valueConvertingService = valueConvertingService;
        this.validationErrorsFormattingService = validationErrorsFormattingService;
        this.userAuthorizationService = userAuthorizationService;
    }

    @GetMapping
    public ResponseEntity<Page<ValueConvertingDto>> getValueConvertings(
            @AuthenticationPrincipal Authentication authentication,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam String sortProperty,
            @RequestParam Sort.Direction sortDirection,
            @RequestParam(required = false) boolean excludeConvertingMap
    ) {
        PageRequest pageRequest = PageRequest
                .of(page, size)
                .withSort(sortDirection, sortProperty);

        Set<Long> sourceApplicationIds = userAuthorizationService.getUserAuthorizedSourceApplicationIds(authentication);
        return ResponseEntity.ok(valueConvertingService.findAllBySourceApplicationIds(
                pageRequest,
                excludeConvertingMap,
                sourceApplicationIds
        ));
    }

    @GetMapping("{valueConvertingId}")
    public ResponseEntity<ValueConvertingDto> getValueConverting(
            @AuthenticationPrincipal Authentication authentication,
            @PathVariable Long valueConvertingId
    ) {
        Optional<ValueConvertingDto> valueConvertingDtoOptional = valueConvertingService
                .findById(valueConvertingId);

        if (valueConvertingDtoOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        ValueConvertingDto valueConvertingDto = valueConvertingDtoOptional.get();

        userAuthorizationService.checkIfUserHasAccessToSourceApplication(
                authentication,
                valueConvertingDto.getFromApplicationId());

        return ResponseEntity.ok(valueConvertingDto);
    }

    @PostMapping
    public ResponseEntity<ValueConvertingDto> postValueConverting(
            @AuthenticationPrincipal Authentication authentication,
            @RequestBody ValueConvertingDto valueConvertingDto
    ) {
        userAuthorizationService.checkIfUserHasAccessToSourceApplication(
                authentication,
                valueConvertingDto.getFromApplicationId());

        Set<ConstraintViolation<ValueConvertingDto>> constraintViolations = validator
                .validate(valueConvertingDto);
        if (!constraintViolations.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    validationErrorsFormattingService.format(constraintViolations)
            );
        }

        return ResponseEntity.ok(valueConvertingService.save(valueConvertingDto));
    }

}
