package no.novari.value.converting.infrastructure.persistence

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import no.novari.value.converting.application.ValueConversionFilter
import no.novari.value.converting.domain.ValueConversion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class ValueConversionSpecificationsTest {
    @Test
    fun `matching filter should scope source applications to intersection of authorized and requested ids`() {
        val root = mock<Root<ValueConversion>>()
        val query = mock<CriteriaQuery<*>>()
        val criteriaBuilder = mock<CriteriaBuilder>()
        val sourceApplicationIdPath = mock<Path<Long>>()
        val sourceApplicationIdPredicate = mock<Predicate>()
        val andPredicate = mock<Predicate>()

        whenever(root.get<Long>("fromApplicationId")).thenReturn(sourceApplicationIdPath)
        whenever(sourceApplicationIdPath.`in`(setOf(2L))).thenReturn(sourceApplicationIdPredicate)
        whenever(criteriaBuilder.and(sourceApplicationIdPredicate)).thenReturn(andPredicate)

        val predicate =
            ValueConversionSpecifications
                .matchingFilter(
                    authorizedSourceApplicationIds = setOf(1L, 2L),
                    filter = ValueConversionFilter(sourceApplicationIds = setOf(2L, 99L)),
                ).toPredicate(root, query, criteriaBuilder)

        assertThat(predicate).isSameAs(andPredicate)
        verify(sourceApplicationIdPath).`in`(setOf(2L))
    }

    @Test
    fun `matching filter should return disjunction when requested ids are outside authorized ids`() {
        val root = mock<Root<ValueConversion>>()
        val query = mock<CriteriaQuery<*>>()
        val criteriaBuilder = mock<CriteriaBuilder>()
        val disjunction = mock<Predicate>()

        whenever(criteriaBuilder.disjunction()).thenReturn(disjunction)

        val predicate =
            ValueConversionSpecifications
                .matchingFilter(
                    authorizedSourceApplicationIds = setOf(1L, 2L),
                    filter = ValueConversionFilter(sourceApplicationIds = setOf(99L)),
                ).toPredicate(root, query, criteriaBuilder)

        assertThat(predicate).isSameAs(disjunction)
        verifyNoInteractions(root)
    }
}
