package com.leapfinance.infopulse.collector.controller

import com.leapfinance.infopulse.collector.dto.*
import com.leapfinance.infopulse.collector.service.LogIngestionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

/**
 * Controller for log ingestion from tracking clients.
 * Requires service API key authentication.
 */
@RestController
@RequestMapping("/api/v1/logs")
@Tag(name = "Log Ingestion", description = "APIs for ingesting logs from tracking clients")
class LogIngestionController(
    private val logIngestionService: LogIngestionService
) {
    
    @PostMapping("/batch")
    @Operation(summary = "Ingest a batch of API logs")
    fun ingestLogs(@RequestBody request: LogBatchRequest): ResponseEntity<ApiResponse<Map<String, Int>>> {
        logger.debug { "Received batch of ${request.logs.size} logs" }
        
        val count = logIngestionService.ingestLogs(request)
        
        return ResponseEntity.ok(
            ApiResponse.success(
                mapOf("ingested" to count),
                "Successfully ingested $count logs"
            )
        )
    }
    
    @PostMapping("/rate-limit-events")
    @Operation(summary = "Ingest a batch of rate limit events")
    fun ingestRateLimitEvents(
        @RequestBody request: RateLimitEventBatchRequest
    ): ResponseEntity<ApiResponse<Map<String, Int>>> {
        logger.debug { "Received batch of ${request.events.size} rate limit events" }
        
        val count = logIngestionService.ingestRateLimitEvents(request)
        
        return ResponseEntity.ok(
            ApiResponse.success(
                mapOf("ingested" to count),
                "Successfully ingested $count rate limit events"
            )
        )
    }
    
    @PostMapping("/single")
    @Operation(summary = "Ingest a single API log")
    fun ingestSingleLog(@RequestBody request: ApiLogRequest): ResponseEntity<ApiResponse<Map<String, Int>>> {
        val count = logIngestionService.ingestLogs(LogBatchRequest(listOf(request)))
        
        return ResponseEntity.ok(
            ApiResponse.success(
                mapOf("ingested" to count),
                "Successfully ingested log"
            )
        )
    }
}
