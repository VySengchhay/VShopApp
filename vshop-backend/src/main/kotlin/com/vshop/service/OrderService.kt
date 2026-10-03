package com.vshop.service

import com.vshop.config.ShopProperties
import com.vshop.dto.CreateOrderRequest
import com.vshop.dto.OrderItemResponse
import com.vshop.dto.OrderResponse
import com.vshop.dto.PaymentSummary
import com.vshop.dto.ShippingResponse
import com.vshop.entity.OrderItem
import com.vshop.entity.OrderStatus
import com.vshop.entity.Payment
import com.vshop.entity.PaymentStatus
import com.vshop.entity.ShopOrder
import com.vshop.exception.badRequest
import com.vshop.exception.conflict
import com.vshop.exception.notFound
import com.vshop.repository.AddressRepository
import com.vshop.repository.OrderRepository
import com.vshop.repository.PaymentRepository
import com.vshop.repository.ProductRepository
import com.vshop.repository.UserRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderService(
    private val orders: OrderRepository,
    private val products: ProductRepository,
    private val addresses: AddressRepository,
    private val users: UserRepository,
    private val payments: PaymentRepository,
    private val paymentService: PaymentService,
    private val shop: ShopProperties,
) {

    @Transactional
    fun create(userId: Long, req: CreateOrderRequest): OrderResponse {
        val address = addresses.findByIdAndUserId(req.addressId, userId) ?: throw notFound("Address")

        // Merge duplicate lines (same product twice in the request)
        val qtyByProduct = req.items.groupBy { it.productId }.mapValues { (_, lines) -> lines.sumOf { it.quantity } }
        val found = products.findAllById(qtyByProduct.keys).associateBy { it.id!! }

        val order = ShopOrder(
            orderNo = newOrderNo(),
            user = users.getReferenceById(userId),
            status = OrderStatus.PENDING_PAYMENT,
            subtotal = BigDecimal.ZERO, deliveryFee = BigDecimal.ZERO, total = BigDecimal.ZERO,
            shipFullName = address.fullName, shipPhone = address.phone, shipAddress = address.oneLine(),
        )

        qtyByProduct.forEach { (productId, qty) ->
            val p = found[productId]?.takeIf { it.active } ?: throw badRequest("Product $productId is no longer available")
            if (p.stock < qty) throw conflict("Only ${p.stock} left of \"${p.name}\"")
            val line = p.price.multiply(BigDecimal(qty)).money()
            order.addItem(OrderItem(p, p.name, p.imageUrl, p.price, qty, line))
        }

        order.subtotal = order.items.fold(BigDecimal.ZERO) { acc, i -> acc + i.lineTotal }.money()
        order.deliveryFee = if (order.subtotal >= shop.freeDeliveryOver) BigDecimal.ZERO.money() else shop.deliveryFee.money()
        order.total = (order.subtotal + order.deliveryFee).money()

        return toResponse(orders.save(order))
    }

    @Transactional(readOnly = true)
    fun list(userId: Long, status: OrderStatus?): List<OrderResponse> {
        val list = if (status == null) orders.findByUserIdOrderByCreatedAtDesc(userId)
                   else orders.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status)
        return list.map { toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun get(userId: Long, orderId: Long) = toResponse(orders.findByIdAndUserId(orderId, userId) ?: throw notFound("Order"))

    @Transactional
    fun cancel(userId: Long, orderId: Long): OrderResponse {
        val order = orders.findByIdAndUserId(orderId, userId) ?: throw notFound("Order")
        if (order.status != OrderStatus.PENDING_PAYMENT) throw conflict("Only unpaid orders can be cancelled")

        // The customer might have paid a second ago in ABA Mobile. Ask PayWay before cancelling.
        payments.findAllForOrder(orderId).filter { it.status == PaymentStatus.PENDING }.forEach {
            paymentService.refresh(it.tranId)
        }
        if (order.status == OrderStatus.PAID) throw conflict("This order was just paid, so it can't be cancelled")

        payments.findAllForOrder(orderId).filter { it.status == PaymentStatus.PENDING }.forEach {
            it.status = PaymentStatus.CANCELLED
            it.updatedAt = Instant.now()
        }
        order.status = OrderStatus.CANCELLED
        order.updatedAt = Instant.now()
        return toResponse(order)
    }

    private fun toResponse(o: ShopOrder): OrderResponse {
        val latest: Payment? = payments.findAllForOrder(o.id!!).firstOrNull()
        return OrderResponse(
            id = o.id!!, orderNo = o.orderNo, status = o.status,
            items = o.items.map { OrderItemResponse(it.product.id!!, it.productName, it.imageUrl, it.unitPrice, it.quantity, it.lineTotal) },
            itemCount = o.items.sumOf { it.quantity },
            subtotal = o.subtotal, deliveryFee = o.deliveryFee, total = o.total, currency = o.currency,
            shipping = ShippingResponse(o.shipFullName, o.shipPhone, o.shipAddress),
            createdAt = o.createdAt, paidAt = o.paidAt,
            latestPayment = latest?.let { PaymentSummary(it.tranId, it.status, it.expiresAt, it.paidAt) },
        )
    }

    private fun newOrderNo(): String {
        val date = DateTimeFormatter.ofPattern("yyMMdd").withZone(ZoneId.of("Asia/Phnom_Penh")).format(Instant.now())
        return "VS$date${Random.nextInt(100_000, 999_999)}"   // e.g. VS260925483920
    }

    private fun BigDecimal.money(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
}
