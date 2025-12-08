package com.leapfinance.infopulse.collector.controller

import com.leapfinance.infopulse.collector.dto.*
import com.leapfinance.infopulse.collector.service.AlertService
import com.leapfinance.infopulse.collector.service.IncidentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

/**
 * Controller for incident management.
 */
@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incidents", description = "APIs for incident management")
class IncidentController(
    private val incidentService: IncidentService
) {
    
    @GetMapping
    @Operation(summary = "Get incidents with filters")
    fun getIncidents(
        @ModelAttribute filter: IncidentFilterRequest
    ): ResponseEntity<ApiResponse<PagedResponse<IncidentResponse>>> {
        val incidents = incidentService.getIncidents(filter)
        return ResponseEntity.ok(ApiResponse.success(incidents))
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get incident by ID")
    fun getIncidentById(@PathVariable id: String): ResponseEntity<ApiResponse<IncidentResponse>> {
        val incident = incidentService.getIncidentById(id)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(ApiResponse.success(incident))
    }
    
    @PostMapping("/{id}/resolve")
    @Operation(summary = "Resolve an incident")
    fun resolveIncident(
        @PathVariable id: String,
        @RequestParam version: Long,
        @RequestBody(required = false) request: ResolveIncidentRequest?,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<IncidentResponse>> {
        return try {
            val resolved = incidentService.resolveIncident(
                id = id,
                expectedVersion = version,
                resolvedBy = authentication.name,
                resolutionNotes = request?.resolutionNotes
            )
            ResponseEntity.ok(ApiResponse.success(resolved, "Incident resolved successfully"))
        } catch (e: OptimisticLockingFailureException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.error("Incident was modified by another user. Please refresh and try again.")
            )
        }
    }
}

/**
 * Controller for alert management.
 */
@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "APIs for alert management")
class AlertController(
    private val alertService: AlertService
) {
    
    @GetMapping
    @Operation(summary = "Get all alerts")
    fun getAlerts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<AlertResponse>>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"))
        val alerts = alertService.getAlerts(pageable)
        return ResponseEntity.ok(ApiResponse.success(alerts))
    }
    
    @GetMapping("/unacknowledged")
    @Operation(summary = "Get unacknowledged alerts")
    fun getUnacknowledgedAlerts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<PagedResponse<AlertResponse>>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"))
        val alerts = alertService.getUnacknowledgedAlerts(pageable)
        return ResponseEntity.ok(ApiResponse.success(alerts))
    }
    
    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge an alert")
    fun acknowledgeAlert(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<AlertResponse>> {
        val alert = alertService.acknowledgeAlert(id, authentication.name)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(ApiResponse.success(alert, "Alert acknowledged"))
    }
}
