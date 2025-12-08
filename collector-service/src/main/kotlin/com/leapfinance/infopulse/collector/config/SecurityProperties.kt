package com.leapfinance.infopulse.collector.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "security")
data class SecurityProperties(
    val jwt: JwtProperties = JwtProperties(),
    val serviceApiKey: String = "infopulse-service-api-key-2024"
)

data class JwtProperties(
    val secret: String = "leap-finance-super-secret-key-for-jwt-tokens-2024",
    val expirationMs: Long = 86400000, // 24 hours
    val refreshExpirationMs: Long = 604800000 // 7 days
)
