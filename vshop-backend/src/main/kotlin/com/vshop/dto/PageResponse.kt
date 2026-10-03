package com.vshop.dto

import org.springframework.data.domain.Page

/** Simple paging shape that is easy to parse on Android. */
data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun <E, T> from(page: Page<E>, mapper: (E) -> T) = PageResponse(
            items = page.content.map(mapper),
            page = page.number,
            size = page.size,
            totalItems = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
        )
    }
}
