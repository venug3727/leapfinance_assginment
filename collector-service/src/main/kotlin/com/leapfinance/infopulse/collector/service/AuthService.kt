package com.leapfinance.infopulse.collector.service

import com.leapfinance.infopulse.collector.dto.LoginRequest
import com.leapfinance.infopulse.collector.dto.LoginResponse
import com.leapfinance.infopulse.collector.repository.meta.UserRepository
import com.leapfinance.infopulse.collector.security.JwtTokenProvider
import mu.KotlinLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder
) {
    
    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByUsername(request.username)
            ?: throw IllegalArgumentException("Invalid username or password")
        
        if (!passwordEncoder.matches(request.password, user.password)) {
            logger.warn { "Failed login attempt for user: ${request.username}" }
            throw IllegalArgumentException("Invalid username or password")
        }
        
        val accessToken = jwtTokenProvider.generateToken(user.username, user.role.name)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.username)
        
        logger.info { "User ${user.username} logged in successfully" }
        
        return LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = 86400, // 24 hours in seconds
            username = user.username,
            role = user.role.name
        )
    }
    
    fun refreshToken(refreshToken: String): LoginResponse {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw IllegalArgumentException("Invalid refresh token")
        }
        
        val username = jwtTokenProvider.getUsernameFromToken(refreshToken)
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found")
        
        val newAccessToken = jwtTokenProvider.generateToken(user.username, user.role.name)
        val newRefreshToken = jwtTokenProvider.generateRefreshToken(user.username)
        
        return LoginResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = 86400,
            username = user.username,
            role = user.role.name
        )
    }
}
