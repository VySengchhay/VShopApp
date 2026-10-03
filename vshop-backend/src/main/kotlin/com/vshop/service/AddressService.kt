package com.vshop.service

import com.vshop.dto.AddressRequest
import com.vshop.dto.AddressResponse
import com.vshop.dto.toResponse
import com.vshop.entity.Address
import com.vshop.exception.notFound
import com.vshop.repository.AddressRepository
import com.vshop.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AddressService(private val addresses: AddressRepository, private val users: UserRepository) {

    @Transactional(readOnly = true)
    fun list(userId: Long) = addresses.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).map { it.toResponse() }

    @Transactional
    fun create(userId: Long, req: AddressRequest): AddressResponse {
        val isFirst = addresses.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).isEmpty()
        val makeDefault = req.isDefault || isFirst
        if (makeDefault) clearDefault(userId)
        val a = Address(
            users.getReferenceById(userId), req.fullName.trim(), req.phone.trim(), req.province.trim(),
            req.district.trim(), req.commune.trim(), req.street.trim(), req.note?.trim(), makeDefault,
        )
        return addresses.save(a).toResponse()
    }

    @Transactional
    fun update(userId: Long, id: Long, req: AddressRequest): AddressResponse {
        val a = addresses.findByIdAndUserId(id, userId) ?: throw notFound("Address")
        if (req.isDefault && !a.defaultAddress) clearDefault(userId)
        a.fullName = req.fullName.trim(); a.phone = req.phone.trim(); a.province = req.province.trim()
        a.district = req.district.trim(); a.commune = req.commune.trim(); a.street = req.street.trim()
        a.note = req.note?.trim(); a.defaultAddress = req.isDefault || a.defaultAddress
        return a.toResponse()
    }

    @Transactional
    fun delete(userId: Long, id: Long) {
        val a = addresses.findByIdAndUserId(id, userId) ?: throw notFound("Address")
        addresses.delete(a)
    }

    private fun clearDefault(userId: Long) =
        addresses.findByUserIdAndDefaultAddressTrue(userId).forEach { it.defaultAddress = false }
}
