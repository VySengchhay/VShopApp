package com.vshop.service

import com.vshop.dto.CategoryResponse
import com.vshop.dto.PageResponse
import com.vshop.dto.ProductResponse
import com.vshop.dto.toResponse
import com.vshop.entity.Category
import com.vshop.entity.Product
import com.vshop.exception.badRequest
import com.vshop.exception.notFound
import com.vshop.repository.CategoryRepository
import com.vshop.repository.ProductRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CatalogService(
    private val categories: CategoryRepository,
    private val products: ProductRepository,
) {
    @Transactional(readOnly = true)
    fun categories() = categories.findAll(Sort.by("sortOrder")).map { CategoryResponse(it.id!!, it.name, it.icon) }

    @Transactional(readOnly = true)
    fun search(categoryId: Long?, query: String?, sort: String, page: Int, size: Int): PageResponse<ProductResponse> {
        if (page < 0 || size !in 1..50) throw badRequest("page must be >= 0 and size between 1 and 50")

        var spec = Specification<Product> { root, _, cb -> cb.isTrue(root.get("active")) }
        if (categoryId != null) {
            spec = spec.and { root, _, cb -> cb.equal(root.get<Category>("category").get<Long>("id"), categoryId) }
        }
        if (!query.isNullOrBlank()) {
            val like = "%${query.trim().lowercase()}%"
            spec = spec.and { root, _, cb -> cb.like(cb.lower(root.get("name")), like) }
        }
        val order = when (sort) {
            "price_asc" -> Sort.by("price").ascending()
            "price_desc" -> Sort.by("price").descending()
            "newest" -> Sort.by("createdAt").descending().and(Sort.by("id").descending())
            else -> throw badRequest("sort must be one of: newest, price_asc, price_desc")
        }
        val result = products.findAll(spec, PageRequest.of(page, size, order))
        return PageResponse.from(result) { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun product(id: Long): ProductResponse {
        val p = products.findById(id).orElseThrow { notFound("Product") }
        if (!p.active) throw notFound("Product")
        return p.toResponse()
    }
}
