package com.leapfinance.infopulse.collector.controller

import com.leapfinance.infopulse.collector.dto.ApiResponse
import com.leapfinance.infopulse.collector.service.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

/**
 * Controller for API Health Score endpoints.
 */
@RestController
@RequestMapping("/api/v1/health-score")
@Tag(name = "Health Score", description = "APIs for health score metrics and analysis")
class HealthScoreController(
    private val healthScoreService: HealthScoreService
) {
    
    @GetMapping("/system")
    @Operation(summary = "Get overall system health score")
    fun getSystemHealthScore(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: Instant?
    ): ResponseEntity<ApiResponse<SystemHealthScore>> {
        val score = healthScoreService.getSystemHealthScore(startTime, endTime)
        return ResponseEntity.ok(ApiResponse.success(score))
    }
    
    @GetMapping("/endpoints")
    @Operation(summary = "Get health scores for all endpoints")
    fun getEndpointHealthScores(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: Instant?
    ): ResponseEntity<ApiResponse<List<EndpointHealthScore>>> {
        val scores = healthScoreService.getEndpointHealthScores(startTime, endTime)
        return ResponseEntity.ok(ApiResponse.success(scores))
    }
    
    @GetMapping("/endpoint")
    @Operation(summary = "Get health score for a specific endpoint")
    fun getEndpointHealthScore(
        @RequestParam serviceName: String,
        @RequestParam endpoint: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: Instant?
    ): ResponseEntity<ApiResponse<EndpointHealthScore>> {
        val score = healthScoreService.getEndpointHealthScore(serviceName, endpoint, startTime, endTime)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ApiResponse.success(score))
    }
    
    @GetMapping("/trend")
    @Operation(summary = "Get health score trend over time")
    fun getHealthScoreTrend(
        @RequestParam(defaultValue = "24") hours: Int,
        @RequestParam(defaultValue = "60") intervalMinutes: Int
    ): ResponseEntity<ApiResponse<List<HealthScoreTrendPoint>>> {
        val trend = healthScoreService.getHealthScoreTrend(hours, intervalMinutes)
        return ResponseEntity.ok(ApiResponse.success(trend))
    }
}
