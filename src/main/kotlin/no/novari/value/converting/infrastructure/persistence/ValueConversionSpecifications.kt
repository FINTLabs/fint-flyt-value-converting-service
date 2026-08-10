package no.novari.value.converting.infrastructure.persistence

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import no.novari.value.converting.application.ValueConversionFilter
import no.novari.value.converting.domain.ValueConversion
import org.springframework.data.jpa.domain.Specification
import java.time.Instant
import java.util.UUID

object ValueConversionSpecifications {
    fun matchingFilter(
        authorizedSourceApplicationIds: Set<Long>,
        filter: ValueConversionFilter,
    ): Specification<ValueConversion> =
        Specification { root, _, criteriaBuilder ->
            val sourceApplicationIds = authorizedSourceApplicationIds.filteredBy(filter.sourceApplicationIds)

            if (sourceApplicationIds.isEmpty()) {
                criteriaBuilder.disjunction()
            } else {
                val predicates =
                    mutableListOf<Predicate>(
                        root.get<Long>("fromApplicationId").`in`(sourceApplicationIds),
                    )

                filter.fromTypeId?.let {
                    predicates += equalIgnoreCase(root, criteriaBuilder, "fromTypeId", it)
                }
                filter.toApplicationId?.let {
                    predicates += equalIgnoreCase(root, criteriaBuilder, "toApplicationId", it)
                }
                filter.toTypeId?.let {
                    predicates += equalIgnoreCase(root, criteriaBuilder, "toTypeId", it)
                }
                filter.displayName?.let {
                    predicates += containsDisplayNameIgnoreCase(root, criteriaBuilder, it)
                }
                filter.createdBy?.let {
                    predicates += actorOidEquals(root, criteriaBuilder, "createdBy", it)
                }
                filter.createdAtFrom?.let {
                    predicates += greaterThanOrEqualToInstant(root, criteriaBuilder, "createdAt", it)
                }
                filter.createdAtTo?.let {
                    predicates += lessThanOrEqualToInstant(root, criteriaBuilder, "createdAt", it)
                }
                filter.modifiedBy?.let {
                    predicates += actorOidEquals(root, criteriaBuilder, "lastModifiedBy", it)
                }
                filter.modifiedAtFrom?.let {
                    predicates += greaterThanOrEqualToInstant(root, criteriaBuilder, "lastModifiedAt", it)
                }
                filter.modifiedAtTo?.let {
                    predicates += lessThanOrEqualToInstant(root, criteriaBuilder, "lastModifiedAt", it)
                }

                criteriaBuilder.and(*predicates.toTypedArray())
            }
        }

    private fun equalIgnoreCase(
        root: Root<ValueConversion>,
        criteriaBuilder: CriteriaBuilder,
        property: String,
        value: String,
    ): Predicate =
        criteriaBuilder.equal(
            criteriaBuilder.lower(root.get(property)),
            value.lowercase(),
        )

    private fun containsDisplayNameIgnoreCase(
        root: Root<ValueConversion>,
        criteriaBuilder: CriteriaBuilder,
        value: String,
    ): Predicate =
        criteriaBuilder.like(
            criteriaBuilder.lower(root.get("displayName")),
            "%${escapeLike(value.lowercase())}%",
            '\\',
        )

    private fun actorOidEquals(
        root: Root<ValueConversion>,
        criteriaBuilder: CriteriaBuilder,
        property: String,
        oid: UUID,
    ): Predicate =
        criteriaBuilder.equal(
            criteriaBuilder.function(
                "jsonb_extract_path_text",
                String::class.java,
                root.get<Any>(property),
                criteriaBuilder.literal("oid"),
            ),
            oid.toString(),
        )

    private fun greaterThanOrEqualToInstant(
        root: Root<ValueConversion>,
        criteriaBuilder: CriteriaBuilder,
        property: String,
        value: Instant,
    ): Predicate =
        criteriaBuilder.greaterThanOrEqualTo(
            root.get(property),
            value,
        )

    private fun lessThanOrEqualToInstant(
        root: Root<ValueConversion>,
        criteriaBuilder: CriteriaBuilder,
        property: String,
        value: Instant,
    ): Predicate =
        criteriaBuilder.lessThanOrEqualTo(
            root.get(property),
            value,
        )

    private fun escapeLike(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

    private fun Set<Long>.filteredBy(requestedSourceApplicationIds: Set<Long>): Set<Long> =
        if (requestedSourceApplicationIds.isEmpty()) {
            this
        } else {
            intersect(requestedSourceApplicationIds)
        }
}
