package no.fintlabs.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Getter
@Builder
@EqualsAndHashCode
@Jacksonized
public class ValueConvertingDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private final Long id;

    @NotNull
    private final String displayName;

    @NotNull
    private final Long fromApplicationId;

    @NotNull
    private final String fromTypeId;

    @NotNull
    private final String toApplicationId;

    @NotNull
    private final String toTypeId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @NotNull
    private final Map<String, String> convertingMap;
}
