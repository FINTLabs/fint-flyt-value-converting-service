package no.novari.value.converting.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import no.novari.flyt.audit.entity.AuditedEntity
import org.hibernate.envers.Audited

@Entity
@Audited
@Table(name = "value_converting")
class ValueConversion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    var id: Long? = null,
    @field:NotNull
    var displayName: String? = null,
    @field:NotNull
    var fromApplicationId: Long? = null,
    @field:NotNull
    var fromTypeId: String? = null,
    @field:NotNull
    var toApplicationId: String? = null,
    @field:NotNull
    var toTypeId: String? = null,
    @field:NotNull
    @ElementCollection
    @CollectionTable(
        name = "converting_map",
        joinColumns = [JoinColumn(name = "value_converting_id")],
    )
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    var convertingMap: MutableMap<String, String> = mutableMapOf(),
) : AuditedEntity()
