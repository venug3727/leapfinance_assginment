package com.leapfinance.infopulse.collector.security

import com.leapfinance.infopulse.collector.config.SecurityProperties
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

private val logger = KotlinLogging.logger {}

@Component
class JwtTokenProvider(
    private val securityProperties: SecurityProperties
) {
    
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(securityProperties.jwt.secret.toByteArray())
    }
    
    fun generateToken(username: String, role: String): String {
        val now = Date()
        val expiryDate = Date(now.time + securityProperties.jwt.expirationMs)
        
        return Jwts.builder()
            .subject(username)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key, Jwts.SIG.HS256)
            .compact()
    }
    
    fun generateRefreshToken(username: String): String {
        val now = Date()
        val expiryDate = Date(now.time + securityProperties.jwt.refreshExpirationMs)
        
        return Jwts.builder()
            .subject(username)
            .claim("type", "refresh")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key, Jwts.SIG.HS256)
            .compact()
    }
    
    fun getUsernameFromToken(token: String): String {
        return getClaims(token).subject
    }
    
    fun getRoleFromToken(token: String): String {
        return getClaims(token).get("role", String::class.java) ?: "VIEWER"
    }
    
    fun validateToken(token: String): Boolean {
        return try {
            getClaims(token)
            true
        } catch (e: JwtException) {
            logger.error { "Invalid JWT token: ${e.message}" }
            false
        } catch (e: IllegalArgumentException) {
            logger.error { "JWT claims string is empty: ${e.message}" }
            false
        }
    }
    
    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
