package com.leapfinance.infopulse.tracking.collector

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.leapfinance.infopulse.tracking.config.MonitoringProperties
import com.leapfinance.infopulse.tracking.model.ApiLogEntry
import com.leapfinance.infopulse.tracking.model.RateLimitEvent
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.util.retry.Retry
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

private val logger = KotlinLogging.logger {}

/**
 * Async client for sending logs to the collector service.
 * Uses WebClient for non-blocking HTTP calls and maintains a buffer queue
 * to handle high-volume logging without blocking the main application.
 */
@Component
class CollectorClient(
    private val properties: MonitoringProperties
) {
    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
    }
    
    private lateinit var webClient: WebClient
    
    // Buffer queues for async processing
    private val logQueue = LinkedBlockingQueue<ApiLogEntry>(10000)
    private val rateLimitQueue = LinkedBlockingQueue<RateLimitEvent>(1000)
    
    private val isRunning = AtomicBoolean(false)
    private var processingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    @PostConstruct
    fun init() {
        if (!properties.enabled) {
            logger.info { "Monitoring is disabled, collector client will not start" }
            return
        }
        
        webClient = WebClient.builder()
            .baseUrl(properties.collector.url)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("X-Service-Api-Key", properties.collector.apiKey)
            .defaultHeader("X-Service-Name", properties.service.name)
            .build()
        
        isRunning.set(true)
        startProcessing()
        
        logger.info { 
            "Collector client initialized - URL: ${properties.collector.url}, Service: ${properties.service.name}" 
        }
    }
    
    @PreDestroy
    fun shutdown() {
        logger.info { "Shutting down collector client..." }
        isRunning.set(false)
        processingJob?.cancel()
        
        // Flush remaining logs
        runBlocking {
            flushLogs()
            flushRateLimitEvents()
        }
        
        scope.cancel()
        logger.info { "Collector client shutdown complete" }
    }
    
    /**
     * Queue an API log entry for async sending.
     * This method is non-blocking and returns immediately.
     */
    fun sendLog(entry: ApiLogEntry) {
        if (!properties.enabled) return
        
        if (!logQueue.offer(entry)) {
            logger.warn { "Log queue full, dropping log entry for ${entry.endpoint}" }
        }
    }
    
    /**
     * Queue a rate limit event for async sending.
     */
    fun sendRateLimitEvent(event: RateLimitEvent) {
        if (!properties.enabled) return
        
        if (!rateLimitQueue.offer(event)) {
            logger.warn { "Rate limit queue full, dropping event for ${event.endpoint}" }
        }
    }
    
    private fun startProcessing() {
        processingJob = scope.launch {
            while (isRunning.get()) {
                try {
                    // Process logs in batches
                    val logBatch = mutableListOf<ApiLogEntry>()
                    var log = logQueue.poll(100, TimeUnit.MILLISECONDS)
                    while (log != null && logBatch.size < 100) {
                        logBatch.add(log)
                        log = logQueue.poll()
                    }
                    
                    if (logBatch.isNotEmpty()) {
                        sendLogBatch(logBatch)
                    }
                    
                    // Process rate limit events
                    val rateLimitBatch = mutableListOf<RateLimitEvent>()
                    var event = rateLimitQueue.poll()
                    while (event != null && rateLimitBatch.size < 50) {
                        rateLimitBatch.add(event)
                        event = rateLimitQueue.poll()
                    }
                    
                    if (rateLimitBatch.isNotEmpty()) {
                        sendRateLimitBatch(rateLimitBatch)
                    }
                    
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    logger.error(e) { "Error processing log queue" }
                }
            }
        }
    }
    
    private suspend fun sendLogBatch(logs: List<ApiLogEntry>) {
        try {
            webClient.post()
                .uri("/api/v1/logs/batch")
                .bodyValue(mapOf("logs" to logs))
                .retrieve()
                .toBodilessEntity()
                .retryWhen(
                    Retry.backoff(
                        properties.collector.retryAttempts.toLong(),
                        Duration.ofMillis(properties.collector.retryDelay)
                    ).filter { it is WebClientResponseException && it.statusCode.is5xxServerError }
                )
                .block()
            
            logger.debug { "Successfully sent ${logs.size} log entries" }
        } catch (e: Exception) {
            // Log but don't crash - resilience is key
            logger.error(e) { "Failed to send log batch of ${logs.size} entries" }
        }
    }
    
    private suspend fun sendRateLimitBatch(events: List<RateLimitEvent>) {
        try {
            webClient.post()
                .uri("/api/v1/logs/rate-limit-events")
                .bodyValue(mapOf("events" to events))
                .retrieve()
                .toBodilessEntity()
                .retryWhen(
                    Retry.backoff(
                        properties.collector.retryAttempts.toLong(),
                        Duration.ofMillis(properties.collector.retryDelay)
                    ).filter { it is WebClientResponseException && it.statusCode.is5xxServerError }
                )
                .block()
            
            logger.debug { "Successfully sent ${events.size} rate limit events" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to send rate limit events batch of ${events.size}" }
        }
    }
    
    private suspend fun flushLogs() {
        val remainingLogs = mutableListOf<ApiLogEntry>()
        logQueue.drainTo(remainingLogs)
        if (remainingLogs.isNotEmpty()) {
            sendLogBatch(remainingLogs)
        }
    }
    
    private suspend fun flushRateLimitEvents() {
        val remainingEvents = mutableListOf<RateLimitEvent>()
        rateLimitQueue.drainTo(remainingEvents)
        if (remainingEvents.isNotEmpty()) {
            sendRateLimitBatch(remainingEvents)
        }
    }
}
