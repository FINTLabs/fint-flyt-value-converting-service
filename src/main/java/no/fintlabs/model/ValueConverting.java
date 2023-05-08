package no.fintlabs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class ValueConverting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private long id;

    @NotNull
    private String displayName;

    @NotNull
    private Long fromApplicationId;

    @NotNull
    private String fromTypeId;

    @NotNull
    private String toApplicationId;

    @NotNull
    private String toTypeId;

    @NotNull
    @ElementCollection
    @CollectionTable(name = "converting_map")
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    private Map<String, String> convertingMap;
}
