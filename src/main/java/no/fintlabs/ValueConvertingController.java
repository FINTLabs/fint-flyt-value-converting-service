package no.fintlabs;

import no.fintlabs.model.ValueConvertingDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static no.fintlabs.resourceserver.UrlPaths.INTERNAL_API;

@RestController
@RequestMapping(INTERNAL_API + "/value-convertings")
public class ValueConvertingController {

    private final ValueConvertingService valueConvertingService;
    private final ValidationErrorsFormattingService validationErrorsFormattingService;
    private final Validator validator;

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
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam String sortProperty,
            @RequestParam Sort.Direction sortDirection,
            @RequestParam(required = false) boolean excludeConvertingMap
    ) {

        PageRequest pageRequest = PageRequest
                .of(page, size)
                .withSort(sortDirection, sortProperty);

        return ResponseEntity.ok(valueConvertingService.findAll(pageRequest, excludeConvertingMap));
    }

    @GetMapping("{valueConvertingId}")
    public ResponseEntity<ValueConvertingDto> getValueConverting(
            @PathVariable Long valueConvertingId
    ) {
        return ResponseEntity.ok(
                valueConvertingService
                        .findById(valueConvertingId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
        );
    }

    @PostMapping
    public ResponseEntity<ValueConvertingDto> postValueConverting(
            @RequestBody ValueConvertingDto valueConvertingDto
    ) {
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
