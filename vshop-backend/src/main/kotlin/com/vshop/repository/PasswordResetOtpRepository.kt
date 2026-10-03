package com.vshop.repository

import com.vshop.entity.PasswordResetOtp
import java.time.Instant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PasswordResetOtpRepository : JpaRepository<PasswordResetOtp, Long> {
    /** Codes that can still be used, newest first. */
    @Query("select o from PasswordResetOtp o where o.user.id = :userId and o.usedAt is null order by o.createdAt desc, o.id desc")
    fun findActive(userId: Long): List<PasswordResetOtp>

    /** How many codes were created for this user since [since] (rate limit). */
    @Query("select count(o) from PasswordResetOtp o where o.user.id = :userId and o.createdAt > :since")
    fun countCreatedSince(userId: Long, since: Instant): Long
}
