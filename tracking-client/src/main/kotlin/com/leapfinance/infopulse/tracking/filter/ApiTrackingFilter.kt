package com.leapfinance.infopulse.tracking.filter

import com.leapfinance.infopulse.tracking.collector.CollectorClient
import com.leapfinance.infopulse.tracking.config.MonitoringProperties
import com.leapfinance.infopulse.tracking.model.ApiLogEntry
import com.leapfinance.infopulse.tracking.model.RateLimitEvent
import com.leapfinance.infopulse.tracking.ratelimit.RateLimiter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Filter that intercepts all HTTP requests to track API metrics.
 * Uses OncePerRequestFilter to ensure accurate response size measurement.
 * 
 * Key features:
 * - Non-blocking: All logging is done asynchronously
 * - Resilient: Filter errors don't crash the main application
 * - Rate limiting: Implements soft rate limiting with Bucket4j
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiTrackingFilter(
    private val properties: MonitoringProperties,
    private val collectorClient: CollectorClient,
    private val rateLimiter: RateLimiter
) : OncePerRequestFilter() {

    companion object {
        private val EXCLUDED_PATHS = setOf(
            "/actuator",
            "/health",
            "/metrics",
            "/favicon.ico",
            "/swagger",
            "/v3/api-docs",
            "/webjars"
        )
        
        private val SENSITIVE_HEADERS = setOf(
            "authorization",
            "cookie",
            "x-api-key",
            "x-service-api-key"
        )
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        if (!properties.enabled) return true
        
        val path = request.requestURI
        return EXCLUDED_PATHS.any { path.startsWith(it) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val startTime = System.currentTimeMillis()
        val timestamp = Instant.now()
        val traceId = request.getHeader("X-Trace-Id") ?: UUID.randomUUID().toString()
        
        // Wrap request/response for content caching
        val wrappedRequest = if (request is ContentCachingRequestWrapper) {
            request
        } else {
            ContentCachingRequestWrapper(request)
        }
        
        val wrappedResponse = if (response is ContentCachingResponseWrapper) {
            response
        } else {
            ContentCachingResponseWrapper(response)
        }
        
        // Check rate limit (soft limit - request proceeds regardless)
        val rateLimitResult = try {
            rateLimiter.checkRateLimit(properties.service.name)
        } catch (e: Exception) {
            logger.error("Rate limiter error, proceeding without limit check", e)
            null
        }
        
        // Log rate limit event if exceeded
        if (rateLimitResult?.exceeded == true) {
            try {
                val rateLimitEvent = RateLimitEvent(
                    serviceName = properties.service.name,
                    endpoint = request.requestURI,
                    method = request.method,
                    timestamp = timestamp,
                    configuredLimit = properties.rateLimit.limit,
                    clientIp = getClientIp(request)
                )
                collectorClient.sendRateLimitEvent(rateLimitEvent)
            } catch (e: Exception) {
                logger.error("Failed to send rate limit event", e)
            }
        }
        
        var errorMessage: String? = null
        
        try {
            // Continue with the request - NEVER block the main flow
            filterChain.doFilter(wrappedRequest, wrappedResponse)
        } catch (e: Exception) {
            errorMessage = e.message
            throw e
        } finally {
            try {
                // Calculate metrics
                val latency = System.currentTimeMillis() - startTime
                val requestSize = wrappedRequest.contentAsByteArray.size.toLong()
                val responseSize = wrappedResponse.contentSize.toLong()
                
                // Build log entry
                val logEntry = ApiLogEntry(
                    endpoint = request.requestURI,
                    method = request.method,
                    requestSize = requestSize,
                    responseSize = responseSize,
                    statusCode = wrappedResponse.status,
                    timestamp = timestamp,
                    latency = latency,
                    serviceName = properties.service.name,
                    traceId = traceId,
                    clientIp = getClientIp(request),
                    userAgent = request.getHeader("User-Agent"),
                    requestHeaders = getFilteredHeaders(request),
                    errorMessage = errorMessage
                )
                
                // Send asynchronously - non-blocking
                collectorClient.sendLog(logEntry)
                
                logger.debug { 
                    "Tracked: ${logEntry.method} ${logEntry.endpoint} - " +
                    "Status: ${logEntry.statusCode}, Latency: ${logEntry.latency}ms"
                }
                
            } catch (e: Exception) {
                // Never let tracking errors affect the main application
                logger.error("Error tracking request, ignoring", e)
            }
            
            // Copy body to response (required for ContentCachingResponseWrapper)
            wrappedResponse.copyBodyToResponse()
        }
    }
    
    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (!xForwardedFor.isNullOrBlank()) {
            xForwardedFor.split(",")[0].trim()
        } else {
            request.remoteAddr
        }
    }
    
    private fun getFilteredHeaders(request: HttpServletRequest): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        request.headerNames?.toList()?.forEach { headerName ->
            if (headerName.lowercase() !in SENSITIVE_HEADERS) {
                headers[headerName] = request.getHeader(headerName)
            }
        }
        return headers
    }
}
