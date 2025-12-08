package com.leapfinance.infopulse.collector.controller

import com.leapfinance.infopulse.collector.dto.*
import com.leapfinance.infopulse.collector.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

/**
 * Controller for authentication.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "APIs for user authentication")
class AuthController(
    private val authService: AuthService
) {
    
    @PostMapping("/login")
    @Operation(summary = "Login with username and password")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        return try {
            val response = authService.login(request)
            ResponseEntity.ok(ApiResponse.success(response, "Login successful"))
        } catch (e: IllegalArgumentException) {
            logger.warn { "Login failed: ${e.message}" }
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.error(e.message ?: "Authentication failed")
            )
        }
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        return try {
            val response = authService.refreshToken(request.refreshToken)
            ResponseEntity.ok(ApiResponse.success(response, "Token refreshed"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.error(e.message ?: "Invalid refresh token")
            )
        }
    }
}
