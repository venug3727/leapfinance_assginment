package com.leapfinance.infopulse.collector.entity.meta

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * User entity for authentication.
 * Stored in meta_db.users collection.
 */
@Document(collection = "users")
data class UserEntity(
    @Id
    val id: String? = null,
    
    @Indexed(unique = true)
    val username: String,
    
    @Indexed(unique = true)
    val email: String,
    
    val password: String,
    
    val role: UserRole = UserRole.DEVELOPER,
    
    val createdAt: Instant = Instant.now(),
    
    val updatedAt: Instant = Instant.now()
)

enum class UserRole {
    ADMIN,
    DEVELOPER,
    VIEWER
}

/**
 * Incident entity representing a slow/broken API issue.
 * Stored in meta_db.incidents collection.
 * 
 * Uses @Version for optimistic locking to handle concurrent updates
 * when multiple developers try to resolve the same incident.
 */
@Document(collection = "incidents")
@CompoundIndexes(
    CompoundIndex(name = "service_endpoint_idx", def = "{'serviceName': 1, 'endpoint': 1}"),
    CompoundIndex(name = "status_created_idx", def = "{'status': 1, 'createdAt': -1}")
)
data class IncidentEntity(
    @Id
    val id: String? = null,
    
    @Indexed
    val serviceName: String,
    
    @Indexed
    val endpoint: String,
    
    val method: String,
    
    val incidentType: IncidentType,
    
    @Indexed
    val status: IncidentStatus = IncidentStatus.OPEN,
    
    /** Average latency at time of incident detection */
    val avgLatency: Long? = null,
    
    /** Error rate at time of incident detection */
    val errorRate: Double? = null,
    
    /** Sample error message */
    val sampleErrorMessage: String? = null,
    
    /** Number of occurrences */
    val occurrenceCount: Int = 1,
    
    /** First occurrence timestamp */
    val firstSeenAt: Instant,
    
    /** Last occurrence timestamp */
    val lastSeenAt: Instant,
    
    val createdAt: Instant = Instant.now(),
    
    val updatedAt: Instant = Instant.now(),
    
    /** Who resolved the incident */
    val resolvedBy: String? = null,
    
    /** When was it resolved */
    val resolvedAt: Instant? = null,
    
    /** Resolution notes */
    val resolutionNotes: String? = null,
    
    /**
     * Version field for optimistic locking.
     * This prevents race conditions when multiple developers try to resolve
     * the same incident simultaneously.
     */
    @Version
    val version: Long? = null
)

enum class IncidentType {
    SLOW_API,       // Latency > 500ms
    BROKEN_API,     // 5xx status code
    RATE_LIMIT_HIT  // Rate limit exceeded
}

enum class IncidentStatus {
    OPEN,
    ACKNOWLEDGED,
    RESOLVED
}

/**
 * Alert entity for tracking detected issues.
 * Stored in meta_db.alerts collection.
 */
@Document(collection = "alerts")
@CompoundIndexes(
    CompoundIndex(name = "type_timestamp_idx", def = "{'alertType': 1, 'timestamp': -1}")
)
data class AlertEntity(
    @Id
    val id: String? = null,
    
    @Indexed
    val alertType: AlertType,
    
    val serviceName: String,
    
    val endpoint: String,
    
    val method: String,
    
    val message: String,
    
    @Indexed
    val timestamp: Instant = Instant.now(),
    
    @Indexed
    val acknowledged: Boolean = false,
    
    val acknowledgedBy: String? = null,
    
    val acknowledgedAt: Instant? = null,
    
    /** Reference to related incident if any */
    val incidentId: String? = null,
    
    /** Additional context data */
    val metadata: Map<String, Any>? = null
)

enum class AlertType {
    SLOW_API,           // API latency > 500ms
    ERROR_SPIKE,        // 5xx status code
    RATE_LIMIT_EXCEEDED // Rate limit hit
}

/**
 * Rate limiter configuration override per service.
 * Stored in meta_db.rate_limiter_configs collection.
 */
@Document(collection = "rate_limiter_configs")
data class RateLimiterConfigEntity(
    @Id
    val id: String? = null,
    
    @Indexed(unique = true)
    val serviceName: String,
    
    val limit: Int,
    
    val windowSeconds: Long = 1,
    
    val enabled: Boolean = true,
    
    val createdAt: Instant = Instant.now(),
    
    val updatedAt: Instant = Instant.now(),
    
    val updatedBy: String? = null
)

/**
 * Audit trail for incident resolutions.
 * Stored in meta_db.resolution_audit collection.
 */
@Document(collection = "resolution_audit")
data class ResolutionAuditEntity(
    @Id
    val id: String? = null,
    
    @Indexed
    val incidentId: String,
    
    val previousStatus: IncidentStatus,
    
    val newStatus: IncidentStatus,
    
    val changedBy: String,
    
    val changedAt: Instant = Instant.now(),
    
    val notes: String? = null
)
