package com.vshop.repository

import com.vshop.entity.Payment
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface PaymentRepository : JpaRepository<Payment, Long> {

    @Query("select p from Payment p where p.order.id = :orderId order by p.createdAt desc, p.id desc")
    fun findAllForOrder(orderId: Long): List<Payment>

    /**
     * Locks the payment row until the transaction ends.
     * The app's polling and PayWay's callback can arrive at the same moment;
     * the lock makes the second one wait, so an order is never marked paid twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.tranId = :tranId")
    fun findByTranIdForUpdate(tranId: String): Payment?
}
