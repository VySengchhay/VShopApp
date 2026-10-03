package com.vshop

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

// We use our own JWT login, so the default in-memory user is turned off.
@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
@ConfigurationPropertiesScan
@EnableScheduling   // daily clean-up of expired refresh tokens
class VShopApplication

fun main(args: Array<String>) {
    runApplication<VShopApplication>(*args)
}
