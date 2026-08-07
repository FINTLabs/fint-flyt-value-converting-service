package no.novari.value.converting.infrastructure.persistence

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import no.novari.value.converting.application.ValueConversionFilter
import no.novari.value.converting.domain.ValueConversion
import org.hibernate.query.criteria.JpaExpression
import org.springframework.data.jpa.domain.Specification
import java.time.Instant

object ValueConversionSpecifications {
    fun matchingFilter(
        authorizedSourceApplicationIds: Set<Long>,
        filter: ValueConversionFilter,
    ): Specification<ValueConversion> =
        Specification { root, _, criteriaBuilder ->
            if (authorizedSourceApplicationIds.isEmpty()) {
                criteriaBuilder.disjunction()
            } else {
                val normalizedFilter = filter.normalized()
                val predicates =
                    mutableListOf<Predicate>(
                        root.get<Long>("fromApplicationId").`in`(authorizedSourceApplicationIds),
                    )

                if (normalizedFilter.sourceApplicationIds.isNotEmpty()) {
                    predicates += root.get<Long>("fromApplicationId").`in`(normalizedFilter.sourceApplicationIds)
                }

                normalizedFilter.fromTypeId?.let {
                    predicates += equalIgnoreCase(root, criteriaBuilder, "fromTypeId", it)
                }
                normalizedFilter.toApplicationId?.let {
                    predicates += equalIgnoreCase(root, criteriaBuilder, "toApplicationId", it)
                }
                normalizedFilter.toTypeId?.let {
                    predicates += equalIgnoreCase(root, criteriaBuilder, "toTypeId", it)
                }
                normalizedFilter.displayName?.let {
                    predicates += containsDisplayNameIgnoreCase(root, criteriaBuilder, it)
                }
                normalizedFilter.createdBy?.let {
                    predicates += containsActor(root, criteriaBuilder, "createdBy", it)
                }
                normalizedFilter.createdAtFrom?.let {
                    predicates += greaterThanOrEqualToInstant(root, criteriaBuilder, "createdAt", it)
                }
                normalizedFilter.createdAtTo?.let {
                    predicates += lessThanOrEqualToInstant(root, criteriaBuilder, "createdAt", it)
                }
                normalizedFilter.modifiedBy?.let {
                    predicates += containsActor(root, criteriaBuilder, "lastModifiedBy", it)
                }
                normalizedFilter.modifiedAtFrom?.let {
                    predicates += greaterThanOrEqualToInstant(root, criteriaBuilder, "lastModifiedAt", it)
                }
                normalizedFilter.modifiedAtTo?.let {
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

    private fun containsActor(
        root: Root<ValueConversion>,
        criteriaBuilder: CriteriaBuilder,
        property: String,
        value: String,
    ): Predicate {
        val actorAsText = (root.get<Any>(property) as JpaExpression<*>).cast(String::class.java)

        return criteriaBuilder.like(
            criteriaBuilder.lower(actorAsText),
            "%${escapeLike(value.lowercase())}%",
            '\\',
        )
    }

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
}
