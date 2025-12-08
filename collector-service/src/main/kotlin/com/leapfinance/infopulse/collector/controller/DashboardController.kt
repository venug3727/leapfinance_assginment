package com.leapfinance.infopulse.collector.controller

import com.leapfinance.infopulse.collector.dto.*
import com.leapfinance.infopulse.collector.service.DashboardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

/**
 * Controller for dashboard APIs.
 * Requires JWT authentication.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "APIs for dashboard data and analytics")
class DashboardController(
    private val dashboardService: DashboardService
) {
    
    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary metrics")
    fun getSummary(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: Instant?
    ): ResponseEntity<ApiResponse<DashboardSummary>> {
        val summary = dashboardService.getSummary(startTime, endTime)
        return ResponseEntity.ok(ApiResponse.success(summary))
    }
    
    @GetMapping("/top-slow-endpoints")
    @Operation(summary = "Get top N slowest endpoints")
    fun getTopSlowEndpoints(
        @RequestParam(defaultValue = "5") limit: Int,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: Instant?
    ): ResponseEntity<ApiResponse<List<EndpointStats>>> {
        val endpoints = dashboardService.getTopSlowEndpoints(limit, startTime, endTime)
        return ResponseEntity.ok(ApiResponse.success(endpoints))
    }
    
    @GetMapping("/error-rate-graph")
    @Operation(summary = "Get error rate graph data")
    fun getErrorRateGraph(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: Instant?,
        @RequestParam(defaultValue = "60") intervalMinutes: Int
    ): ResponseEntity<ApiResponse<ErrorRateGraphData>> {
        val graphData = dashboardService.getErrorRateGraph(startTime, endTime, intervalMinutes)
        return ResponseEntity.ok(ApiResponse.success(graphData))
    }
    
    @GetMapping("/logs")
    @Operation(summary = "Get API logs with filters")
    fun getLogs(
        @RequestParam(required = false) serviceName: String?,
        @RequestParam(required = false) endpoint: String?,
        @RequestParam(required = false) method: String?,
        @RequestParam(required = false) statusCode: Int?,
        @RequestParam(required = false) minLatency: Long?,
        @RequestParam(required = false) maxLatency: Long?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: Instant?,
        @RequestParam(required = false) isSlow: Boolean?,
        @RequestParam(required = false) isBroken: Boolean?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<ApiLogResponse>>> {
        val filter = LogFilterRequest(
            serviceName = serviceName,
            endpoint = endpoint,
            method = method,
            statusCode = statusCode,
            minLatency = minLatency,
            maxLatency = maxLatency,
            startTime = startTime,
            endTime = endTime,
            isSlow = isSlow,
            isBroken = isBroken,
            page = page,
            size = size
        )
        
        val logs = dashboardService.getLogs(filter)
        return ResponseEntity.ok(ApiResponse.success(logs))
    }
    
    @GetMapping("/services")
    @Operation(summary = "Get list of distinct services")
    fun getServices(): ResponseEntity<ApiResponse<List<String>>> {
        val services = dashboardService.getServices()
        return ResponseEntity.ok(ApiResponse.success(services))
    }
    
    @GetMapping("/endpoints")
    @Operation(summary = "Get list of distinct endpoints")
    fun getEndpoints(
        @RequestParam(required = false) serviceName: String?
    ): ResponseEntity<ApiResponse<List<String>>> {
        val endpoints = dashboardService.getEndpoints(serviceName)
        return ResponseEntity.ok(ApiResponse.success(endpoints))
    }
}
