package no.novari.value.converting.infrastructure.persistence

import no.novari.value.converting.domain.ValueConversion
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ValueConversionRepository : JpaRepository<ValueConversion, Long> {
    fun findAllByFromApplicationIdIn(
        pageable: Pageable,
        fromApplicationIds: Set<Long>,
    ): Page<ValueConversion>
}
