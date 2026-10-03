package com.vshop.repository

import com.vshop.entity.Address
import org.springframework.data.jpa.repository.JpaRepository

interface AddressRepository : JpaRepository<Address, Long> {
    fun findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId: Long): List<Address>
    fun findByIdAndUserId(id: Long, userId: Long): Address?
    fun findByUserIdAndDefaultAddressTrue(userId: Long): List<Address>
}
