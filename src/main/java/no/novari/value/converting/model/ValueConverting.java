package no.novari.value.converting.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
