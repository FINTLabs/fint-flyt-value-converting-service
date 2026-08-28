package no.novari.flyt.value.converting.api.dto

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

data class ValueConversionPageResponse(
    val content: List<ValueConversionResponse>,
    val pageable: ValueConversionPageableResponse,
    val last: Boolean,
    val totalPages: Int,
    val totalElements: Long,
    val first: Boolean,
    val size: Int,
    val number: Int,
    val sort: ValueConversionSortResponse,
    val numberOfElements: Int,
    val empty: Boolean,
) {
    companion object {
        fun from(page: Page<ValueConversionResponse>): ValueConversionPageResponse =
            ValueConversionPageResponse(
                content = page.content,
                pageable = ValueConversionPageableResponse.from(page.pageable),
                last = page.isLast,
                totalPages = page.totalPages,
                totalElements = page.totalElements,
                first = page.isFirst,
                size = page.size,
                number = page.number,
                sort = ValueConversionSortResponse.from(page.sort),
                numberOfElements = page.numberOfElements,
                empty = page.isEmpty,
            )
    }
}

data class ValueConversionPageableResponse(
    val pageNumber: Int,
    val pageSize: Int,
    val offset: Long,
    val sort: ValueConversionSortResponse,
    val paged: Boolean,
    val unpaged: Boolean,
) {
    companion object {
        fun from(pageable: Pageable): ValueConversionPageableResponse =
            ValueConversionPageableResponse(
                pageNumber = if (pageable.isPaged) pageable.pageNumber else 0,
                pageSize = if (pageable.isPaged) pageable.pageSize else 0,
                offset = if (pageable.isPaged) pageable.offset else 0,
                sort = ValueConversionSortResponse.from(pageable.sort),
                paged = pageable.isPaged,
                unpaged = pageable.isUnpaged,
            )
    }
}

data class ValueConversionSortResponse(
    val empty: Boolean,
    val sorted: Boolean,
    val unsorted: Boolean,
) {
    companion object {
        fun from(sort: Sort): ValueConversionSortResponse =
            ValueConversionSortResponse(
                empty = sort.isEmpty,
                sorted = sort.isSorted,
                unsorted = sort.isUnsorted,
            )
    }
}
