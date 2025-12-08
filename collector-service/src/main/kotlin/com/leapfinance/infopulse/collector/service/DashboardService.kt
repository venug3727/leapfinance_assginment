package com.leapfinance.infopulse.collector.service

import com.leapfinance.infopulse.collector.dto.*
import com.leapfinance.infopulse.collector.entity.logs.ApiLogEntity
import com.leapfinance.infopulse.collector.entity.meta.IncidentStatus
import com.leapfinance.infopulse.collector.repository.logs.ApiLogRepository
import com.leapfinance.infopulse.collector.repository.logs.RateLimitEventRepository
import com.leapfinance.infopulse.collector.repository.meta.AlertRepository
import com.leapfinance.infopulse.collector.repository.meta.IncidentRepository
import mu.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

private val logger = KotlinLogging.logger {}

/**
 * Service for dashboard data and analytics.
 */
@Service
class DashboardService(
    private val apiLogRepository: ApiLogRepository,
    private val rateLimitEventRepository: RateLimitEventRepository,
    private val incidentRepository: IncidentRepository,
    private val alertRepository: AlertRepository
) {
    
    /**
     * Get dashboard summary metrics.
     */
    fun getSummary(startTime: Instant? = null, endTime: Instant? = null): DashboardSummary {
        val effectiveStartTime = startTime ?: Instant.now().minus(24, ChronoUnit.HOURS)
        val effectiveEndTime = endTime ?: Instant.now()
        
        val pageable = PageRequest.of(0, 1)
        val allLogs = apiLogRepository.findWithFilters(
            startTime = effectiveStartTime,
            endTime = effectiveEndTime,
            pageable = PageRequest.of(0, Int.MAX_VALUE)
        )
        
        val totalRequests = allLogs.totalElements
        val slowApiCount = apiLogRepository.countSlowApis(effectiveStartTime, effectiveEndTime)
        val brokenApiCount = apiLogRepository.countBrokenApis(effectiveStartTime, effectiveEndTime)
        val rateLimitViolations = rateLimitEventRepository.countByServiceName(
            startTime = effectiveStartTime,
            endTime = effectiveEndTime
        )
        
        val avgLatency = if (allLogs.content.isNotEmpty()) {
            allLogs.content.map { it.latency }.average()
        } else 0.0
        
        val openIncidents = incidentRepository.countByStatus(IncidentStatus.OPEN)
        val unacknowledgedAlerts = alertRepository.countUnacknowledged()
        
        return DashboardSummary(
            totalRequests = totalRequests,
            slowApiCount = slowApiCount,
            brokenApiCount = brokenApiCount,
            rateLimitViolations = rateLimitViolations,
            averageLatency = avgLatency,
            openIncidents = openIncidents,
            unacknowledgedAlerts = unacknowledgedAlerts
        )
    }
    
    /**
     * Get top N slowest endpoints.
     */
    fun getTopSlowEndpoints(
        limit: Int = 5,
        startTime: Instant? = null,
        endTime: Instant? = null
    ): List<EndpointStats> {
        val stats = apiLogRepository.getAverageLatencyByEndpoint(
            startTime = startTime,
            endTime = endTime,
            limit = limit
        )
        
        return stats.map { stat ->
            EndpointStats(
                endpoint = stat.endpoint,
                serviceName = stat.serviceName,
                avgLatency = stat.avgLatency,
                maxLatency = stat.maxLatency,
                requestCount = stat.requestCount,
                errorRate = null
            )
        }
    }
    
    /**
     * Get error rate data for graph.
     */
    fun getErrorRateGraph(
        startTime: Instant? = null,
        endTime: Instant? = null,
        intervalMinutes: Int = 60
    ): ErrorRateGraphData {
        val effectiveStartTime = startTime ?: Instant.now().minus(24, ChronoUnit.HOURS)
        val effectiveEndTime = endTime ?: Instant.now()
        
        // Get all logs in the time range
        val logs = apiLogRepository.findWithFilters(
            startTime = effectiveStartTime,
            endTime = effectiveEndTime,
            pageable = PageRequest.of(0, Int.MAX_VALUE)
        ).content
        
        // Group by time intervals
        val dataPoints = mutableListOf<TimeSeriesDataPoint>()
        var currentTime = effectiveStartTime
        
        while (currentTime.isBefore(effectiveEndTime)) {
            val intervalEnd = currentTime.plus(intervalMinutes.toLong(), ChronoUnit.MINUTES)
            
            val logsInInterval = logs.filter { log ->
                log.timestamp.isAfter(currentTime) && log.timestamp.isBefore(intervalEnd)
            }
            
            val errorRate = if (logsInInterval.isNotEmpty()) {
                val errorCount = logsInInterval.count { it.isBroken }
                (errorCount.toDouble() / logsInInterval.size) * 100
            } else 0.0
            
            dataPoints.add(
                TimeSeriesDataPoint(
                    timestamp = currentTime,
                    value = errorRate,
                    label = "Error Rate %"
                )
            )
            
            currentTime = intervalEnd
        }
        
        return ErrorRateGraphData(
            dataPoints = dataPoints,
            interval = "${intervalMinutes}m"
        )
    }
    
    /**
     * Get logs with filters.
     */
    fun getLogs(filter: LogFilterRequest): PagedResponse<ApiLogResponse> {
        val pageable = PageRequest.of(
            filter.page,
            filter.size,
            Sort.by(Sort.Direction.DESC, "timestamp")
        )
        
        val page = apiLogRepository.findWithFilters(
            serviceName = filter.serviceName,
            endpoint = filter.endpoint,
            method = filter.method,
            statusCode = filter.statusCode,
            minLatency = filter.minLatency,
            maxLatency = filter.maxLatency,
            startTime = filter.startTime,
            endTime = filter.endTime,
            isSlow = filter.isSlow,
            isBroken = filter.isBroken,
            pageable = pageable
        )
        
        return PagedResponse(
            content = page.content.map { it.toResponse() },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            isFirst = page.isFirst,
            isLast = page.isLast
        )
    }
    
    /**
     * Get distinct services.
     */
    fun getServices(): List<String> {
        return apiLogRepository.findDistinctServices()
    }
    
    /**
     * Get distinct endpoints.
     */
    fun getEndpoints(serviceName: String? = null): List<String> {
        return apiLogRepository.findDistinctEndpoints(serviceName)
    }
    
    private fun ApiLogEntity.toResponse() = ApiLogResponse(
        id = id!!,
        endpoint = endpoint,
        method = method,
        requestSize = requestSize,
        responseSize = responseSize,
        statusCode = statusCode,
        timestamp = timestamp,
        latency = latency,
        serviceName = serviceName,
        traceId = traceId,
        clientIp = clientIp,
        userAgent = userAgent,
        isSlow = isSlow,
        isBroken = isBroken,
        errorMessage = errorMessage
    )
}
