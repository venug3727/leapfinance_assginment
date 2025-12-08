package com.leapfinance.infopulse.collector.service

import com.leapfinance.infopulse.collector.dto.AlertResponse
import com.leapfinance.infopulse.collector.dto.ApiLogResponse
import com.leapfinance.infopulse.collector.dto.IncidentResponse
import mu.KotlinLogging
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Service for broadcasting real-time events via WebSocket.
 * 
 * Topics:
 * - /topic/logs: Real-time API log events
 * - /topic/alerts: Real-time alert notifications  
 * - /topic/incidents: Real-time incident updates
 * - /topic/health: Real-time health score updates (every 5s)
 * - /topic/metrics: Real-time metrics updates (every 5s)
 */
@Service
class RealTimeEventService(
    private val messagingTemplate: SimpMessagingTemplate,
    private val healthScoreService: HealthScoreService,
    private val dashboardService: DashboardService
) {
    
    companion object {
        const val TOPIC_LOGS = "/topic/logs"
        const val TOPIC_ALERTS = "/topic/alerts"
        const val TOPIC_INCIDENTS = "/topic/incidents"
        const val TOPIC_HEALTH = "/topic/health"
        const val TOPIC_METRICS = "/topic/metrics"
    }
    
    /**
     * Broadcast a new API log event.
     */
    fun broadcastLog(log: ApiLogResponse) {
        try {
            val event = RealTimeEvent(
                type = EventType.LOG,
                data = log,
                timestamp = Instant.now()
            )
            messagingTemplate.convertAndSend(TOPIC_LOGS, event)
            logger.debug { "Broadcasted log event: ${log.endpoint}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to broadcast log event" }
        }
    }
    
    /**
     * Broadcast a new alert.
     */
    fun broadcastAlert(alert: AlertResponse) {
        try {
            val event = RealTimeEvent(
                type = EventType.ALERT,
                data = alert,
                timestamp = Instant.now()
            )
            messagingTemplate.convertAndSend(TOPIC_ALERTS, event)
            logger.debug { "Broadcasted alert: ${alert.alertType} - ${alert.endpoint}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to broadcast alert" }
        }
    }
    
    /**
     * Broadcast an incident update.
     */
    fun broadcastIncident(incident: IncidentResponse) {
        try {
            val event = RealTimeEvent(
                type = EventType.INCIDENT,
                data = incident,
                timestamp = Instant.now()
            )
            messagingTemplate.convertAndSend(TOPIC_INCIDENTS, event)
            logger.debug { "Broadcasted incident: ${incident.id}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to broadcast incident" }
        }
    }
    
    /**
     * Periodically broadcast health score updates.
     */
    @Scheduled(fixedRate = 5000)
    fun broadcastHealthScore() {
        try {
            val healthScore = healthScoreService.getSystemHealthScore()
            val event = RealTimeEvent(
                type = EventType.HEALTH_UPDATE,
                data = healthScore,
                timestamp = Instant.now()
            )
            messagingTemplate.convertAndSend(TOPIC_HEALTH, event)
            logger.debug { "Broadcasted health score: ${healthScore.overallScore}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to broadcast health score" }
        }
    }
    
    /**
     * Periodically broadcast metrics updates.
     */
    @Scheduled(fixedRate = 5000)
    fun broadcastMetrics() {
        try {
            val summary = dashboardService.getSummary()
            val event = RealTimeEvent(
                type = EventType.METRICS_UPDATE,
                data = MetricsUpdate(
                    totalRequests = summary.totalRequests,
                    slowApiCount = summary.slowApiCount,
                    brokenApiCount = summary.brokenApiCount,
                    rateLimitViolations = summary.rateLimitViolations,
                    averageLatency = summary.averageLatency,
                    openIncidents = summary.openIncidents,
                    unacknowledgedAlerts = summary.unacknowledgedAlerts
                ),
                timestamp = Instant.now()
            )
            messagingTemplate.convertAndSend(TOPIC_METRICS, event)
            logger.debug { "Broadcasted metrics update" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to broadcast metrics" }
        }
    }
}

enum class EventType {
    LOG,
    ALERT,
    INCIDENT,
    HEALTH_UPDATE,
    METRICS_UPDATE
}

data class RealTimeEvent<T>(
    val type: EventType,
    val data: T,
    val timestamp: Instant
)

data class MetricsUpdate(
    val totalRequests: Long,
    val slowApiCount: Long,
    val brokenApiCount: Long,
    val rateLimitViolations: Long,
    val averageLatency: Double,
    val openIncidents: Long,
    val unacknowledgedAlerts: Long
)
