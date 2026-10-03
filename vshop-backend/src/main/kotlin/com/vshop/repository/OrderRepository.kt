package com.vshop.repository

import com.vshop.entity.OrderStatus
import com.vshop.entity.ShopOrder
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<ShopOrder, Long> {
    fun findByIdAndUserId(id: Long, userId: Long): ShopOrder?
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<ShopOrder>
    fun findByUserIdAndStatusOrderByCreatedAtDesc(userId: Long, status: OrderStatus): List<ShopOrder>
}
