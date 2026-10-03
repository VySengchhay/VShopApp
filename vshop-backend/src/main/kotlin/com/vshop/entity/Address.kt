package com.vshop.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "addresses")
class Address(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    var user: User,
    @Column(name = "full_name") var fullName: String,
    var phone: String,
    var province: String,
    var district: String,
    var commune: String,
    var street: String,
    var note: String?,
    @Column(name = "is_default") var defaultAddress: Boolean = false,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    /** One line for receipts and the orders table, e.g. "St 271, Sangkat Tuol Tumpung, Khan Chamkarmon, Phnom Penh". */
    fun oneLine() = listOf(street, commune, district, province).joinToString(", ")
}
