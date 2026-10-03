package com.vshop.service

import com.vshop.repository.RefreshTokenRepository
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** Once a day, delete expired refresh tokens so the table doesn't grow forever. */
@Component
class RefreshTokenCleanup(private val tokens: RefreshTokenRepository) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    fun purgeExpired() {
        val deleted = tokens.deleteAllExpired(Instant.now())
        log.debug("Purged {} expired refresh tokens", deleted)
    }
}
