package com.leapfinance.infopulse.collector.entity.logs

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * Entity representing an API log entry.
 * Stored in logs_db.api_logs collection.
 */
@Document(collection = "api_logs")
@CompoundIndexes(
    CompoundIndex(name = "service_timestamp_idx", def = "{'serviceName': 1, 'timestamp': -1}"),
    CompoundIndex(name = "endpoint_timestamp_idx", def = "{'endpoint': 1, 'timestamp': -1}"),
    CompoundIndex(name = "status_timestamp_idx", def = "{'statusCode': 1, 'timestamp': -1}")
)
data class ApiLogEntity(
    @Id
    val id: String? = null,
    
    @Indexed
    val endpoint: String,
    
    val method: String,
    
    val requestSize: Long,
    
    val responseSize: Long,
    
    @Indexed
    val statusCode: Int,
    
    @Indexed
    val timestamp: Instant,
    
    @Indexed
    val latency: Long,
    
    @Indexed
    val serviceName: String,
    
    val traceId: String? = null,
    
    val clientIp: String? = null,
    
    val userAgent: String? = null,
    
    val requestHeaders: Map<String, String>? = null,
    
    val errorMessage: String? = null,
    
    /** Flag for slow API (latency > 500ms) */
    val isSlow: Boolean = latency > 500,
    
    /** Flag for broken API (5xx status) */
    val isBroken: Boolean = statusCode in 500..599
)

/**
 * Entity representing a rate limit event.
 * Stored in logs_db.rate_limit_events collection.
 */
@Document(collection = "rate_limit_events")
@CompoundIndexes(
    CompoundIndex(name = "service_timestamp_idx", def = "{'serviceName': 1, 'timestamp': -1}")
)
data class RateLimitEventEntity(
    @Id
    val id: String? = null,
    
    @Indexed
    val serviceName: String,
    
    val endpoint: String,
    
    val method: String,
    
    @Indexed
    val timestamp: Instant,
    
    val configuredLimit: Int,
    
    val eventType: String = "rate-limit-hit",
    
    val clientIp: String? = null
)
