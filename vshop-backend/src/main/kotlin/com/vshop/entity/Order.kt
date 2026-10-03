package com.vshop.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

enum class OrderStatus { PENDING_PAYMENT, PAID, SHIPPING, DELIVERED, CANCELLED }

// Named ShopOrder because ORDER is a reserved word in SQL/JPQL.
@Entity(name = "ShopOrder")
@Table(name = "orders")
class ShopOrder(
    @Column(name = "order_no", nullable = false, unique = true)
    var orderNo: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    var user: User,

    @Enumerated(EnumType.STRING)
    var status: OrderStatus,

    var subtotal: BigDecimal,
    @Column(name = "delivery_fee") var deliveryFee: BigDecimal,
    var total: BigDecimal,
    var currency: String = "USD",

    @Column(name = "ship_full_name") var shipFullName: String,
    @Column(name = "ship_phone") var shipPhone: String,
    @Column(name = "ship_address") var shipAddress: String,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<OrderItem> = mutableListOf()

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @Column(name = "paid_at")
    var paidAt: Instant? = null

    fun addItem(item: OrderItem) {
        item.order = this
        items.add(item)
    }
}

@Entity
@Table(name = "order_items")
class OrderItem(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    var product: Product,
    @Column(name = "product_name") var productName: String,
    @Column(name = "image_url") var imageUrl: String?,
    @Column(name = "unit_price") var unitPrice: BigDecimal,
    var quantity: Int,
    @Column(name = "line_total") var lineTotal: BigDecimal,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    lateinit var order: ShopOrder
}
