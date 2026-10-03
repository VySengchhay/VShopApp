package com.vshop.repository

import com.vshop.entity.RefreshToken
import java.time.Instant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByTokenHash(tokenHash: String): RefreshToken?

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.user.id = :userId and r.revoked = false")
    fun revokeAllForUser(userId: Long): Int

    @Modifying
    @Query("delete from RefreshToken r where r.expiryDate < :now")
    fun deleteAllExpired(now: Instant): Int
}
