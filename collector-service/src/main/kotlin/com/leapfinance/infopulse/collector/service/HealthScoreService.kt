package com.leapfinance.infopulse.collector.service

import com.leapfinance.infopulse.collector.dto.*
import com.leapfinance.infopulse.collector.repository.logs.ApiLogRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * Service for calculating API Health Scores.
 * Health Score = weighted combination of:
 * - Availability (40%): Success rate of requests
 * - Latency (30%): How fast APIs respond
 * - Error Rate (30%): 4xx/5xx error rates
 */
@Service
class HealthScoreService(
    private val apiLogRepository: ApiLogRepository
) {
    
    companion object {
        // Weight factors for health score calculation
        const val AVAILABILITY_WEIGHT = 0.40
        const val LATENCY_WEIGHT = 0.30
        const val ERROR_WEIGHT = 0.30
        
        // Thresholds for scoring
        const val EXCELLENT_LATENCY_MS = 100L    // < 100ms = 100 score
        const val GOOD_LATENCY_MS = 300L         // < 300ms = 80 score
        const val ACCEPTABLE_LATENCY_MS = 500L   // < 500ms = 60 score
        const val POOR_LATENCY_MS = 1000L        // < 1000ms = 40 score
        // > 1000ms = 20 score
    }
    
    /**
     * Get overall system health score.
     */
    fun getSystemHealthScore(
        startTime: Instant? = null,
        endTime: Instant? = null
    ): SystemHealthScore {
        val effectiveStartTime = startTime ?: Instant.now().minus(1, ChronoUnit.HOURS)
        val effectiveEndTime = endTime ?: Instant.now()
        
        val allEndpointScores = getEndpointHealthScores(effectiveStartTime, effectiveEndTime)
        
        if (allEndpointScores.isEmpty()) {
            return SystemHealthScore(
                overallScore = 100,
                availabilityScore = 100,
                latencyScore = 100,
                errorScore = 100,
                status = HealthStatus.EXCELLENT,
                endpointScores = emptyList(),
                serviceScores = emptyList(),
                calculatedAt = Instant.now()
            )
        }
        
        // Calculate weighted average of all endpoint scores
        val totalRequests = allEndpointScores.sumOf { it.requestCount }
        val weightedScore = if (totalRequests > 0) {
            allEndpointScores.sumOf { it.healthScore * it.requestCount } / totalRequests
        } else {
            allEndpointScores.map { it.healthScore }.average()
        }
        
        val avgAvailability = allEndpointScores.map { it.availabilityScore }.average()
        val avgLatency = allEndpointScores.map { it.latencyScore }.average()
        val avgError = allEndpointScores.map { it.errorScore }.average()
        
        // Group by service
        val serviceScores = allEndpointScores
            .groupBy { it.serviceName }
            .map { (serviceName, endpoints) ->
                val serviceRequests = endpoints.sumOf { it.requestCount }
                val serviceWeightedScore = if (serviceRequests > 0) {
                    endpoints.sumOf { it.healthScore * it.requestCount } / serviceRequests
                } else {
                    endpoints.map { it.healthScore }.average()
                }
                ServiceHealthScore(
                    serviceName = serviceName,
                    healthScore = serviceWeightedScore.toInt(),
                    status = getHealthStatus(serviceWeightedScore.toInt()),
                    endpointCount = endpoints.size,
                    requestCount = serviceRequests,
                    criticalEndpoints = endpoints.filter { it.status == HealthStatus.CRITICAL }.size,
                    warningEndpoints = endpoints.filter { it.status == HealthStatus.WARNING }.size
                )
            }
            .sortedByDescending { it.requestCount }
        
        return SystemHealthScore(
            overallScore = weightedScore.toInt(),
            availabilityScore = avgAvailability.toInt(),
            latencyScore = avgLatency.toInt(),
            errorScore = avgError.toInt(),
            status = getHealthStatus(weightedScore.toInt()),
            endpointScores = allEndpointScores.sortedBy { it.healthScore }.take(10), // Top 10 worst
            serviceScores = serviceScores,
            calculatedAt = Instant.now()
        )
    }
    
    /**
     * Get health scores for all endpoints.
     */
    fun getEndpointHealthScores(
        startTime: Instant? = null,
        endTime: Instant? = null
    ): List<EndpointHealthScore> {
        val effectiveStartTime = startTime ?: Instant.now().minus(1, ChronoUnit.HOURS)
        val effectiveEndTime = endTime ?: Instant.now()
        
        val endpointStats = apiLogRepository.getEndpointStatistics(
            startTime = effectiveStartTime,
            endTime = effectiveEndTime
        )
        
        return endpointStats.map { stat ->
            calculateEndpointHealthScore(stat)
        }
    }
    
    /**
     * Get health score for a specific endpoint.
     */
    fun getEndpointHealthScore(
        serviceName: String,
        endpoint: String,
        startTime: Instant? = null,
        endTime: Instant? = null
    ): EndpointHealthScore? {
        val effectiveStartTime = startTime ?: Instant.now().minus(1, ChronoUnit.HOURS)
        val effectiveEndTime = endTime ?: Instant.now()
        
        val stats = apiLogRepository.getEndpointStats(
            serviceName = serviceName,
            endpoint = endpoint,
            startTime = effectiveStartTime,
            endTime = effectiveEndTime
        ) ?: return null
        
        return calculateEndpointHealthScore(stats)
    }
    
    /**
     * Get health score trend over time.
     */
    fun getHealthScoreTrend(
        hours: Int = 24,
        intervalMinutes: Int = 60
    ): List<HealthScoreTrendPoint> {
        val now = Instant.now()
        val points = mutableListOf<HealthScoreTrendPoint>()
        
        var currentTime = now.minus(hours.toLong(), ChronoUnit.HOURS)
        
        while (currentTime.isBefore(now)) {
            val intervalEnd = currentTime.plus(intervalMinutes.toLong(), ChronoUnit.MINUTES)
            
            val score = getSystemHealthScore(currentTime, intervalEnd)
            
            points.add(
                HealthScoreTrendPoint(
                    timestamp = currentTime,
                    score = score.overallScore,
                    status = score.status
                )
            )
            
            currentTime = intervalEnd
        }
        
        return points
    }
    
    private fun calculateEndpointHealthScore(stats: EndpointStatistics): EndpointHealthScore {
        // Calculate availability score (based on non-5xx responses)
        val availabilityScore = calculateAvailabilityScore(stats.successRate)
        
        // Calculate latency score
        val latencyScore = calculateLatencyScore(stats.avgLatency)
        
        // Calculate error score (inverse of error rate)
        val errorScore = calculateErrorScore(stats.errorRate)
        
        // Calculate weighted health score
        val healthScore = (
            availabilityScore * AVAILABILITY_WEIGHT +
            latencyScore * LATENCY_WEIGHT +
            errorScore * ERROR_WEIGHT
        ).toInt()
        
        return EndpointHealthScore(
            serviceName = stats.serviceName,
            endpoint = stats.endpoint,
            method = stats.method,
            healthScore = healthScore,
            availabilityScore = availabilityScore,
            latencyScore = latencyScore,
            errorScore = errorScore,
            status = getHealthStatus(healthScore),
            avgLatency = stats.avgLatency,
            p95Latency = stats.p95Latency,
            p99Latency = stats.p99Latency,
            errorRate = stats.errorRate,
            successRate = stats.successRate,
            requestCount = stats.requestCount,
            lastRequestAt = stats.lastRequestAt
        )
    }
    
    private fun calculateAvailabilityScore(successRate: Double): Int {
        // Success rate directly maps to availability score
        return (successRate * 100).toInt().coerceIn(0, 100)
    }
    
    private fun calculateLatencyScore(avgLatency: Double): Int {
        return when {
            avgLatency < EXCELLENT_LATENCY_MS -> 100
            avgLatency < GOOD_LATENCY_MS -> 80 + ((GOOD_LATENCY_MS - avgLatency) / (GOOD_LATENCY_MS - EXCELLENT_LATENCY_MS) * 20).toInt()
            avgLatency < ACCEPTABLE_LATENCY_MS -> 60 + ((ACCEPTABLE_LATENCY_MS - avgLatency) / (ACCEPTABLE_LATENCY_MS - GOOD_LATENCY_MS) * 20).toInt()
            avgLatency < POOR_LATENCY_MS -> 40 + ((POOR_LATENCY_MS - avgLatency) / (POOR_LATENCY_MS - ACCEPTABLE_LATENCY_MS) * 20).toInt()
            else -> max(0, (40 - ((avgLatency - POOR_LATENCY_MS) / 100)).toInt())
        }.coerceIn(0, 100)
    }
    
    private fun calculateErrorScore(errorRate: Double): Int {
        // Lower error rate = higher score
        // 0% error = 100 score, 100% error = 0 score
        return ((1 - errorRate) * 100).toInt().coerceIn(0, 100)
    }
    
    private fun getHealthStatus(score: Int): HealthStatus {
        return when {
            score >= 90 -> HealthStatus.EXCELLENT
            score >= 75 -> HealthStatus.GOOD
            score >= 50 -> HealthStatus.WARNING
            else -> HealthStatus.CRITICAL
        }
    }
}

// DTOs for Health Score

enum class HealthStatus {
    EXCELLENT,  // 90-100
    GOOD,       // 75-89
    WARNING,    // 50-74
    CRITICAL    // 0-49
}

data class SystemHealthScore(
    val overallScore: Int,
    val availabilityScore: Int,
    val latencyScore: Int,
    val errorScore: Int,
    val status: HealthStatus,
    val endpointScores: List<EndpointHealthScore>,  // Top worst endpoints
    val serviceScores: List<ServiceHealthScore>,
    val calculatedAt: Instant
)

data class ServiceHealthScore(
    val serviceName: String,
    val healthScore: Int,
    val status: HealthStatus,
    val endpointCount: Int,
    val requestCount: Long,
    val criticalEndpoints: Int,
    val warningEndpoints: Int
)

data class EndpointHealthScore(
    val serviceName: String,
    val endpoint: String,
    val method: String,
    val healthScore: Int,
    val availabilityScore: Int,
    val latencyScore: Int,
    val errorScore: Int,
    val status: HealthStatus,
    val avgLatency: Double,
    val p95Latency: Long,
    val p99Latency: Long,
    val errorRate: Double,
    val successRate: Double,
    val requestCount: Long,
    val lastRequestAt: Instant
)

data class EndpointStatistics(
    val serviceName: String,
    val endpoint: String,
    val method: String,
    val avgLatency: Double,
    val p95Latency: Long,
    val p99Latency: Long,
    val errorRate: Double,
    val successRate: Double,
    val requestCount: Long,
    val lastRequestAt: Instant
)

data class HealthScoreTrendPoint(
    val timestamp: Instant,
    val score: Int,
    val status: HealthStatus
)
