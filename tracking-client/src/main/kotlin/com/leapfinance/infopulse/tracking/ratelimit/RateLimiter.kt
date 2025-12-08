package com.leapfinance.infopulse.tracking.ratelimit

import com.leapfinance.infopulse.tracking.config.MonitoringProperties
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Rate limiter implementation using Bucket4j.
 * Implements a soft rate limit - requests are allowed to proceed even when limit is hit,
 * but a rate-limit-hit event is logged.
 */
@Component
class RateLimiter(
    private val properties: MonitoringProperties
) {
    private val buckets = ConcurrentHashMap<String, Bucket>()
    
    /**
     * Check if the request should be rate limited.
     * @return true if rate limit is exceeded (but request will still proceed)
     */
    fun checkRateLimit(serviceName: String): RateLimitResult {
        if (!properties.rateLimit.enabled) {
            return RateLimitResult(exceeded = false, bucket = null)
        }
        
        val bucket = buckets.computeIfAbsent(serviceName) { createBucket() }
        val probe = bucket.tryConsumeAndReturnRemaining(1)
        
        return if (probe.isConsumed) {
            RateLimitResult(
                exceeded = false,
                bucket = bucket,
                remainingTokens = probe.remainingTokens
            )
        } else {
            logger.warn { "Rate limit exceeded for service: $serviceName" }
            RateLimitResult(
                exceeded = true,
                bucket = bucket,
                remainingTokens = 0,
                nanosToWait = probe.nanosToWaitForRefill
            )
        }
    }
    
    private fun createBucket(): Bucket {
        val limit = properties.rateLimit.limit.toLong()
        val windowSeconds = properties.rateLimit.windowSeconds
        
        val bandwidth = Bandwidth.builder()
            .capacity(limit)
            .refillGreedy(limit, Duration.ofSeconds(windowSeconds))
            .build()
        
        return Bucket.builder()
            .addLimit(bandwidth)
            .build()
    }
    
    /**
     * Get current rate limit configuration.
     */
    fun getConfiguration(): RateLimitConfiguration {
        return RateLimitConfiguration(
            enabled = properties.rateLimit.enabled,
            limit = properties.rateLimit.limit,
            windowSeconds = properties.rateLimit.windowSeconds
        )
    }
}

data class RateLimitResult(
    val exceeded: Boolean,
    val bucket: Bucket?,
    val remainingTokens: Long = 0,
    val nanosToWait: Long = 0
)

data class RateLimitConfiguration(
    val enabled: Boolean,
    val limit: Int,
    val windowSeconds: Long
)
