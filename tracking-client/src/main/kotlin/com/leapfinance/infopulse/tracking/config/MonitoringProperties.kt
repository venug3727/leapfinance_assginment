package com.leapfinance.infopulse.tracking.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the API Monitoring Tracking Client.
 * 
 * Example application.yaml:
 * ```yaml
 * monitoring:
 *   enabled: true
 *   collector:
 *     url: http://localhost:8080
 *     apiKey: your-service-api-key
 *   service:
 *     name: orders-service
 *   rateLimit:
 *     enabled: true
 *     limit: 100
 * ```
 */
@ConfigurationProperties(prefix = "monitoring")
data class MonitoringProperties(
    /** Enable or disable the tracking client */
    val enabled: Boolean = true,
    
    /** Collector service configuration */
    val collector: CollectorProperties = CollectorProperties(),
    
    /** Service identification */
    val service: ServiceProperties = ServiceProperties(),
    
    /** Rate limiting configuration */
    val rateLimit: RateLimitProperties = RateLimitProperties()
)

data class CollectorProperties(
    /** URL of the collector service */
    val url: String = "http://localhost:8080",
    
    /** API key for service-to-service authentication */
    val apiKey: String = "",
    
    /** Connection timeout in milliseconds */
    val connectionTimeout: Long = 5000,
    
    /** Read timeout in milliseconds */
    val readTimeout: Long = 10000,
    
    /** Retry attempts on failure */
    val retryAttempts: Int = 3,
    
    /** Retry delay in milliseconds */
    val retryDelay: Long = 1000
)

data class ServiceProperties(
    /** Name of the service using this tracking client */
    val name: String = "unknown-service"
)

data class RateLimitProperties(
    /** Enable or disable rate limiting */
    val enabled: Boolean = true,
    
    /** Maximum requests per second */
    val limit: Int = 100,
    
    /** Time window in seconds for rate limiting */
    val windowSeconds: Long = 1
)
