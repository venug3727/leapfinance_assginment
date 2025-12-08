package com.leapfinance.infopulse.collector.dto

import com.leapfinance.infopulse.collector.entity.meta.AlertType
import com.leapfinance.infopulse.collector.entity.meta.IncidentStatus
import com.leapfinance.infopulse.collector.entity.meta.IncidentType
import java.time.Instant

// ==================== Log DTOs ====================

data class ApiLogRequest(
    val endpoint: String,
    val method: String,
    val requestSize: Long,
    val responseSize: Long,
    val statusCode: Int,
    val timestamp: Instant,
    val latency: Long,
    val serviceName: String,
    val traceId: String? = null,
    val clientIp: String? = null,
    val userAgent: String? = null,
    val requestHeaders: Map<String, String>? = null,
    val errorMessage: String? = null
)

data class LogBatchRequest(
    val logs: List<ApiLogRequest>
)

data class RateLimitEventRequest(
    val serviceName: String,
    val endpoint: String,
    val method: String,
    val timestamp: Instant,
    val configuredLimit: Int,
    val eventType: String = "rate-limit-hit",
    val clientIp: String? = null
)

data class RateLimitEventBatchRequest(
    val events: List<RateLimitEventRequest>
)

data class ApiLogResponse(
    val id: String,
    val endpoint: String,
    val method: String,
    val requestSize: Long,
    val responseSize: Long,
    val statusCode: Int,
    val timestamp: Instant,
    val latency: Long,
    val serviceName: String,
    val traceId: String?,
    val clientIp: String?,
    val userAgent: String?,
    val isSlow: Boolean,
    val isBroken: Boolean,
    val errorMessage: String?
)

// ==================== Auth DTOs ====================

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val username: String,
    val role: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

// ==================== Dashboard DTOs ====================

data class DashboardSummary(
    val totalRequests: Long,
    val slowApiCount: Long,
    val brokenApiCount: Long,
    val rateLimitViolations: Long,
    val averageLatency: Double,
    val openIncidents: Long,
    val unacknowledgedAlerts: Long
)

data class EndpointStats(
    val endpoint: String,
    val serviceName: String,
    val avgLatency: Double,
    val maxLatency: Long,
    val requestCount: Long,
    val errorRate: Double?
)

data class TimeSeriesDataPoint(
    val timestamp: Instant,
    val value: Double,
    val label: String? = null
)

data class ErrorRateGraphData(
    val dataPoints: List<TimeSeriesDataPoint>,
    val interval: String
)

// ==================== Incident DTOs ====================

data class IncidentResponse(
    val id: String,
    val serviceName: String,
    val endpoint: String,
    val method: String,
    val incidentType: IncidentType,
    val status: IncidentStatus,
    val avgLatency: Long?,
    val errorRate: Double?,
    val sampleErrorMessage: String?,
    val occurrenceCount: Int,
    val firstSeenAt: Instant,
    val lastSeenAt: Instant,
    val createdAt: Instant,
    val resolvedBy: String?,
    val resolvedAt: Instant?,
    val resolutionNotes: String?,
    val version: Long?
)

data class ResolveIncidentRequest(
    val resolutionNotes: String? = null
)

// ==================== Alert DTOs ====================

data class AlertResponse(
    val id: String,
    val alertType: AlertType,
    val serviceName: String,
    val endpoint: String,
    val method: String,
    val message: String,
    val timestamp: Instant,
    val acknowledged: Boolean,
    val acknowledgedBy: String?,
    val acknowledgedAt: Instant?,
    val incidentId: String?,
    val metadata: Map<String, Any>?
)

// ==================== Filter DTOs ====================

data class LogFilterRequest(
    val serviceName: String? = null,
    val endpoint: String? = null,
    val method: String? = null,
    val statusCode: Int? = null,
    val minLatency: Long? = null,
    val maxLatency: Long? = null,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val isSlow: Boolean? = null,
    val isBroken: Boolean? = null,
    val page: Int = 0,
    val size: Int = 20
)

data class IncidentFilterRequest(
    val serviceName: String? = null,
    val endpoint: String? = null,
    val incidentType: IncidentType? = null,
    val status: IncidentStatus? = null,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val page: Int = 0,
    val size: Int = 20
)

// ==================== Config DTOs ====================

data class RateLimiterConfigRequest(
    val serviceName: String,
    val limit: Int,
    val windowSeconds: Long = 1,
    val enabled: Boolean = true
)

data class RateLimiterConfigResponse(
    val id: String?,
    val serviceName: String,
    val limit: Int,
    val windowSeconds: Long,
    val enabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val updatedBy: String?
)

// ==================== API Response Wrappers ====================

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null
) {
    companion object {
        fun <T> success(data: T, message: String? = null): ApiResponse<T> {
            return ApiResponse(success = true, data = data, message = message)
        }
        
        fun <T> error(error: String): ApiResponse<T> {
            return ApiResponse(success = false, error = error)
        }
    }
}

data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isFirst: Boolean,
    val isLast: Boolean
)
