package no.fintlabs;

import no.fintlabs.model.ValueConvertingDto;
import no.fintlabs.resourceserver.security.user.UserAuthorizationUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static no.fintlabs.resourceserver.UrlPaths.INTERNAL_API;

@RestController
@RequestMapping(INTERNAL_API + "/value-convertings")
public class ValueConvertingController {

    private final ValueConvertingService valueConvertingService;
    private final ValidationErrorsFormattingService validationErrorsFormattingService;
    private final Validator validator;
    @Value("${fint.flyt.resource-server.user-permissions-consumer.enabled:false}")
    private boolean userPermissionsConsumerEnabled;

    public ValueConvertingController(
            ValueConvertingService valueConvertingService,
            ValidationErrorsFormattingService validationErrorsFormattingService,
            ValidatorFactory validatorFactory
    ) {
        this.validator = validatorFactory.getValidator();
        this.valueConvertingService = valueConvertingService;
        this.validationErrorsFormattingService = validationErrorsFormattingService;
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

        if (userPermissionsConsumerEnabled) {
            List<Long> sourceApplicationIds =
                    UserAuthorizationUtil.convertSourceApplicationIdsStringToList(authentication);
            return ResponseEntity.ok(valueConvertingService.findAllBySourceApplicationIds(
                    pageRequest,
                    excludeConvertingMap,
                    sourceApplicationIds
            ));
        }

        return ResponseEntity.ok(valueConvertingService.findAll(pageRequest, excludeConvertingMap));
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

        if (userPermissionsConsumerEnabled) {
            UserAuthorizationUtil.checkIfUserHasAccessToSourceApplication(authentication, valueConvertingDto.getFromApplicationId());
        }

        return ResponseEntity.ok(valueConvertingDto);
    }

    @PostMapping
    public ResponseEntity<ValueConvertingDto> postValueConverting(
            @AuthenticationPrincipal Authentication authentication,
            @RequestBody ValueConvertingDto valueConvertingDto
    ) {
        if (userPermissionsConsumerEnabled) {
            UserAuthorizationUtil.checkIfUserHasAccessToSourceApplication(authentication, valueConvertingDto.getFromApplicationId());
        }

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
