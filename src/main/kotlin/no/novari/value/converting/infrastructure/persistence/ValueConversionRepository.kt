package no.novari.value.converting.infrastructure.persistence

import no.novari.value.converting.domain.ValueConversion
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ValueConversionRepository : JpaRepository<ValueConversion, Long> {
    @Query(
        """
        SELECT DISTINCT valueConversion.fromApplicationId
        FROM ValueConversion valueConversion
        WHERE valueConversion.fromApplicationId IS NOT NULL
        """,
    )
    fun findDistinctSourceApplicationIds(): Set<Long>

    fun findAllByFromApplicationIdIn(
        pageable: Pageable,
        fromApplicationIds: Set<Long>,
    ): Page<ValueConversion>
}
