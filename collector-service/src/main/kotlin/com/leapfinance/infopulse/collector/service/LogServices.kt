package com.leapfinance.infopulse.collector.service

import com.leapfinance.infopulse.collector.dto.*
import com.leapfinance.infopulse.collector.entity.logs.ApiLogEntity
import com.leapfinance.infopulse.collector.entity.logs.RateLimitEventEntity
import com.leapfinance.infopulse.collector.entity.meta.*
import com.leapfinance.infopulse.collector.repository.logs.ApiLogRepository
import com.leapfinance.infopulse.collector.repository.logs.RateLimitEventRepository
import com.leapfinance.infopulse.collector.repository.meta.AlertRepository
import com.leapfinance.infopulse.collector.repository.meta.IncidentRepository
import mu.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant

private val logger = KotlinLogging.logger {}

// Extension function to convert ApiLogEntity to ApiLogResponse
fun ApiLogEntity.toResponse() = ApiLogResponse(
    id = id ?: "",
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

/**
 * Service for ingesting and processing API logs.
 */
@Service
class LogIngestionService(
    private val apiLogRepository: ApiLogRepository,
    private val rateLimitEventRepository: RateLimitEventRepository,
    private val alertService: AlertService,
    private val incidentService: IncidentService,
    private val realTimeEventService: RealTimeEventService
) {
    
    /**
     * Ingest a batch of API logs.
     */
    fun ingestLogs(request: LogBatchRequest): Int {
        logger.debug { "Ingesting batch of ${request.logs.size} logs" }
        
        val entities = request.logs.map { log ->
            ApiLogEntity(
                endpoint = log.endpoint,
                method = log.method,
                requestSize = log.requestSize,
                responseSize = log.responseSize,
                statusCode = log.statusCode,
                timestamp = log.timestamp,
                latency = log.latency,
                serviceName = log.serviceName,
                traceId = log.traceId,
                clientIp = log.clientIp,
                userAgent = log.userAgent,
                requestHeaders = log.requestHeaders,
                errorMessage = log.errorMessage
            )
        }
        
        val saved = apiLogRepository.saveAll(entities)
        
        // Broadcast logs via WebSocket for real-time updates
        saved.forEach { log ->
            realTimeEventService.broadcastLog(log.toResponse())
        }
        
        // Process alerts asynchronously
        processAlertsAsync(saved)
        
        return saved.size
        
        return saved.size
    }
    
    /**
     * Ingest a batch of rate limit events.
     */
    fun ingestRateLimitEvents(request: RateLimitEventBatchRequest): Int {
        logger.debug { "Ingesting batch of ${request.events.size} rate limit events" }
        
        val entities = request.events.map { event ->
            RateLimitEventEntity(
                serviceName = event.serviceName,
                endpoint = event.endpoint,
                method = event.method,
                timestamp = event.timestamp,
                configuredLimit = event.configuredLimit,
                eventType = event.eventType,
                clientIp = event.clientIp
            )
        }
        
        val saved = rateLimitEventRepository.saveAll(entities)
        
        // Create alerts for rate limit events
        saved.forEach { event ->
            alertService.createRateLimitAlert(event)
        }
        
        return saved.size
    }
    
    @Async
    fun processAlertsAsync(logs: List<ApiLogEntity>) {
        logs.forEach { log ->
            try {
                // Check for slow API
                if (log.isSlow) {
                    alertService.createSlowApiAlert(log)
                    incidentService.createOrUpdateIncident(log, IncidentType.SLOW_API)
                }
                
                // Check for broken API
                if (log.isBroken) {
                    alertService.createBrokenApiAlert(log)
                    incidentService.createOrUpdateIncident(log, IncidentType.BROKEN_API)
                }
            } catch (e: Exception) {
                logger.error("Error processing alerts for log ${log.id}", e)
            }
        }
    }
}

/**
 * Service for managing alerts.
 */
@Service
class AlertService(
    private val alertRepository: AlertRepository
) {
    
    fun createSlowApiAlert(log: ApiLogEntity) {
        val alert = AlertEntity(
            alertType = AlertType.SLOW_API,
            serviceName = log.serviceName,
            endpoint = log.endpoint,
            method = log.method,
            message = "API latency ${log.latency}ms exceeds threshold (500ms)",
            timestamp = log.timestamp,
            metadata = mapOf(
                "latency" to log.latency,
                "threshold" to 500,
                "traceId" to (log.traceId ?: "")
            )
        )
        alertRepository.save(alert)
        logger.info { "Created SLOW_API alert for ${log.serviceName}:${log.endpoint}" }
    }
    
    fun createBrokenApiAlert(log: ApiLogEntity) {
        val alert = AlertEntity(
            alertType = AlertType.ERROR_SPIKE,
            serviceName = log.serviceName,
            endpoint = log.endpoint,
            method = log.method,
            message = "API returned error status ${log.statusCode}: ${log.errorMessage ?: "Unknown error"}",
            timestamp = log.timestamp,
            metadata = mapOf(
                "statusCode" to log.statusCode,
                "errorMessage" to (log.errorMessage ?: ""),
                "traceId" to (log.traceId ?: "")
            )
        )
        alertRepository.save(alert)
        logger.info { "Created ERROR_SPIKE alert for ${log.serviceName}:${log.endpoint}" }
    }
    
    fun createRateLimitAlert(event: RateLimitEventEntity) {
        val alert = AlertEntity(
            alertType = AlertType.RATE_LIMIT_EXCEEDED,
            serviceName = event.serviceName,
            endpoint = event.endpoint,
            method = event.method,
            message = "Rate limit exceeded (${event.configuredLimit} req/s)",
            timestamp = event.timestamp,
            metadata = mapOf(
                "configuredLimit" to event.configuredLimit,
                "clientIp" to (event.clientIp ?: "")
            )
        )
        alertRepository.save(alert)
        logger.info { "Created RATE_LIMIT_EXCEEDED alert for ${event.serviceName}:${event.endpoint}" }
    }
    
    fun getAlerts(pageable: PageRequest): PagedResponse<AlertResponse> {
        val page = alertRepository.findAll(pageable)
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
    
    fun getUnacknowledgedAlerts(pageable: PageRequest): PagedResponse<AlertResponse> {
        val page = alertRepository.findUnacknowledged(pageable)
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
    
    fun acknowledgeAlert(id: String, username: String): AlertResponse? {
        return alertRepository.acknowledgeAlert(id, username)?.toResponse()
    }
    
    private fun AlertEntity.toResponse() = AlertResponse(
        id = id!!,
        alertType = alertType,
        serviceName = serviceName,
        endpoint = endpoint,
        method = method,
        message = message,
        timestamp = timestamp,
        acknowledged = acknowledged,
        acknowledgedBy = acknowledgedBy,
        acknowledgedAt = acknowledgedAt,
        incidentId = incidentId,
        metadata = metadata
    )
}

/**
 * Service for managing incidents.
 */
@Service
class IncidentService(
    private val incidentRepository: IncidentRepository
) {
    
    /**
     * Create a new incident or update existing one.
     */
    fun createOrUpdateIncident(log: ApiLogEntity, type: IncidentType) {
        val existing = incidentRepository.findByServiceAndEndpoint(log.serviceName, log.endpoint)
        
        if (existing != null && existing.incidentType == type) {
            // Update existing incident
            incidentRepository.incrementOccurrence(existing.id!!)
            logger.debug { "Updated incident ${existing.id} for ${log.serviceName}:${log.endpoint}" }
        } else {
            // Create new incident
            val incident = IncidentEntity(
                serviceName = log.serviceName,
                endpoint = log.endpoint,
                method = log.method,
                incidentType = type,
                avgLatency = if (type == IncidentType.SLOW_API) log.latency else null,
                sampleErrorMessage = log.errorMessage,
                firstSeenAt = log.timestamp,
                lastSeenAt = log.timestamp
            )
            val saved = incidentRepository.save(incident)
            logger.info { "Created new incident ${saved.id} for ${log.serviceName}:${log.endpoint}" }
        }
    }
    
    fun getIncidents(filter: IncidentFilterRequest): PagedResponse<IncidentResponse> {
        val pageable = PageRequest.of(
            filter.page,
            filter.size,
            Sort.by(Sort.Direction.DESC, "createdAt")
        )
        
        val page = incidentRepository.findWithFilters(
            serviceName = filter.serviceName,
            endpoint = filter.endpoint,
            incidentType = filter.incidentType,
            status = filter.status,
            startTime = filter.startTime,
            endTime = filter.endTime,
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
    
    fun getIncidentById(id: String): IncidentResponse? {
        return incidentRepository.findById(id)?.toResponse()
    }
    
    /**
     * Resolve an incident with optimistic locking.
     */
    fun resolveIncident(
        id: String,
        expectedVersion: Long,
        resolvedBy: String,
        resolutionNotes: String?
    ): IncidentResponse {
        val resolved = incidentRepository.resolveIncident(
            id = id,
            expectedVersion = expectedVersion,
            resolvedBy = resolvedBy,
            resolutionNotes = resolutionNotes
        )
        logger.info { "Incident $id resolved by $resolvedBy" }
        return resolved.toResponse()
    }
    
    private fun IncidentEntity.toResponse() = IncidentResponse(
        id = id!!,
        serviceName = serviceName,
        endpoint = endpoint,
        method = method,
        incidentType = incidentType,
        status = status,
        avgLatency = avgLatency,
        errorRate = errorRate,
        sampleErrorMessage = sampleErrorMessage,
        occurrenceCount = occurrenceCount,
        firstSeenAt = firstSeenAt,
        lastSeenAt = lastSeenAt,
        createdAt = createdAt,
        resolvedBy = resolvedBy,
        resolvedAt = resolvedAt,
        resolutionNotes = resolutionNotes,
        version = version
    )
}
