package com.vshop.dto

import com.vshop.entity.Address
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class AddressRequest(
    @field:NotBlank @field:Size(max = 100) val fullName: String,
    @field:Pattern(regexp = "^(\\+855|0)[1-9][0-9]{7,8}$", message = "Enter a valid Cambodian phone number")
    val phone: String,
    @field:NotBlank @field:Size(max = 80) val province: String,
    @field:NotBlank @field:Size(max = 80) val district: String,
    @field:NotBlank @field:Size(max = 80) val commune: String,
    @field:NotBlank @field:Size(max = 200) val street: String,
    @field:Size(max = 200) val note: String? = null,
    val isDefault: Boolean = false,
)

data class AddressResponse(
    val id: Long, val fullName: String, val phone: String, val province: String, val district: String,
    val commune: String, val street: String, val note: String?, val isDefault: Boolean, val fullAddress: String,
)

fun Address.toResponse() = AddressResponse(
    id!!, fullName, phone, province, district, commune, street, note, defaultAddress, oneLine()
)
