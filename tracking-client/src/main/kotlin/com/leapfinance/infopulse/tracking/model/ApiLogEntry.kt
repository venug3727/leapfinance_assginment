package com.leapfinance.infopulse.tracking.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant

/**
 * Represents an API log entry to be sent to the collector.
 */
data class ApiLogEntry(
    /** The API endpoint path */
    val endpoint: String,
    
    /** HTTP method (GET, POST, PUT, DELETE, etc.) */
    val method: String,
    
    /** Size of the request body in bytes */
    val requestSize: Long,
    
    /** Size of the response body in bytes */
    val responseSize: Long,
    
    /** HTTP status code */
    val statusCode: Int,
    
    /** Timestamp when the request was received */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    val timestamp: Instant,
    
    /** Request processing time in milliseconds */
    val latency: Long,
    
    /** Name of the service that processed the request */
    val serviceName: String,
    
    /** Optional trace ID for distributed tracing */
    val traceId: String? = null,
    
    /** Client IP address */
    val clientIp: String? = null,
    
    /** User agent string */
    val userAgent: String? = null,
    
    /** Request headers (filtered for sensitive data) */
    val requestHeaders: Map<String, String>? = null,
    
    /** Any error message if the request failed */
    val errorMessage: String? = null
)

/**
 * Represents a rate limit event.
 */
data class RateLimitEvent(
    /** Name of the service */
    val serviceName: String,
    
    /** The endpoint that triggered the rate limit */
    val endpoint: String,
    
    /** HTTP method */
    val method: String,
    
    /** Timestamp of the event */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    val timestamp: Instant,
    
    /** Current rate limit configuration */
    val configuredLimit: Int,
    
    /** Event type - always "rate-limit-hit" */
    val eventType: String = "rate-limit-hit",
    
    /** Client IP that triggered the limit */
    val clientIp: String? = null
)

/**
 * Batch of log entries for bulk sending.
 */
data class LogBatch(
    val logs: List<ApiLogEntry>,
    val rateLimitEvents: List<RateLimitEvent> = emptyList()
)
