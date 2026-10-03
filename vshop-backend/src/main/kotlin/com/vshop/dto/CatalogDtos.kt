package com.vshop.dto

import com.vshop.entity.Product
import java.math.BigDecimal

data class CategoryResponse(val id: Long, val name: String, val icon: String?)

data class ProductResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val currency: String,
    val imageUrl: String?,
    val stock: Int,
    val inStock: Boolean,
    val categoryId: Long,
    val categoryName: String,
)

fun Product.toResponse() = ProductResponse(
    id = id!!, name = name, description = description, price = price, currency = "USD",
    imageUrl = imageUrl, stock = stock, inStock = stock > 0,
    categoryId = category.id!!, categoryName = category.name,
)
