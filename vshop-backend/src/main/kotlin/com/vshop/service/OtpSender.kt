package com.vshop.service

import com.vshop.config.OtpProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component


/**
 * Sends the 6-digit code by email.
 * With app.otp.log-only=true (the default) it only prints the code in the server log,
 * so you can test the whole flow without an email account.
 */
@Component
class OtpSender(
    private val mailSender: ObjectProvider<JavaMailSender>,
    private val props: OtpProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun send(email: String, name: String, code: String) {
        val sender = mailSender.ifAvailable
        if (props.logOnly || sender == null) {
            log.warn("[DEV ONLY] Password reset code for {}: {}  (set OTP_LOG_ONLY=false to email it)", email, code)
            return
        }
        val msg = SimpleMailMessage()
        msg.from = props.mailFrom
        msg.setTo(email)
        msg.subject = "Your VShop code: $code"
        msg.text = """
            Hi $name,

            Your code to reset your VShop password is:

                $code

            It expires in ${props.expiryMinutes} minutes. If you didn't ask for this, you can ignore this email.
        """.trimIndent()
        try {
            sender.send(msg)
        } catch (e: MailException) {
            // Don't tell the caller: the response must look the same either way.
            log.error("Couldn't send reset code to {}: {}", email, e.message)
        }
    }
}
