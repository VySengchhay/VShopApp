package com.vshop.controller

import com.vshop.dto.AddressRequest
import com.vshop.security.AuthUser
import com.vshop.service.AddressService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/addresses")
class AddressController(private val service: AddressService) {
    @GetMapping
    fun list(@AuthenticationPrincipal u: AuthUser) = service.list(u.id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal u: AuthUser, @Valid @RequestBody req: AddressRequest) = service.create(u.id, req)

    @PutMapping("/{id}")
    fun update(@AuthenticationPrincipal u: AuthUser, @PathVariable id: Long, @Valid @RequestBody req: AddressRequest) =
        service.update(u.id, id, req)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal u: AuthUser, @PathVariable id: Long) = service.delete(u.id, id)
}
