package no.fintlabs;

import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ValidationErrorsFormattingService {

    public <T> String format(Set<ConstraintViolation<T>> errors) {
        return "Validation error" + (errors.size() > 1 ? "s:" : ":") + " " +
                errors
                        .stream()
                        .map(constraintViolation -> "'" +
                                (constraintViolation.getPropertyPath().toString().isBlank()
                                        ? ""
                                        : constraintViolation.getPropertyPath().toString() + " "
                                ) +
                                constraintViolation.getMessage() +
                                "'"
                        )
                        .sorted(String::compareTo)
                        .collect(Collectors.joining(", ", "[", "]"));
    }
}
