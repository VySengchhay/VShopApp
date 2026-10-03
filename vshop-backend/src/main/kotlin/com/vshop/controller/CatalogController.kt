package com.vshop.controller

import com.vshop.service.CatalogService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class CatalogController(private val catalog: CatalogService) {

    @GetMapping("/categories")
    fun categories() = catalog.categories()

    /** Example: GET /api/products?categoryId=1&q=coffee&sort=price_asc&page=0&size=20 */
    @GetMapping("/products")
    fun products(
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "newest") sort: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = catalog.search(categoryId, q, sort, page, size)

    @GetMapping("/products/{id}")
    fun product(@PathVariable id: Long) = catalog.product(id)
}
