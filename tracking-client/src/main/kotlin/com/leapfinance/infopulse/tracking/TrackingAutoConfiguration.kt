package com.leapfinance.infopulse.tracking

import com.leapfinance.infopulse.tracking.collector.CollectorClient
import com.leapfinance.infopulse.tracking.config.MonitoringProperties
import com.leapfinance.infopulse.tracking.filter.ApiTrackingFilter
import com.leapfinance.infopulse.tracking.ratelimit.RateLimiter
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

private val logger = KotlinLogging.logger {}

/**
 * Auto-configuration for the API Tracking Client.
 * This configuration is automatically picked up by Spring Boot when the library is on the classpath.
 * 
 * To disable tracking, set `monitoring.enabled=false` in application.yaml
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "monitoring", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MonitoringProperties::class)
class TrackingAutoConfiguration {

    @Bean
    fun rateLimiter(properties: MonitoringProperties): RateLimiter {
        logger.info { 
            "Initializing rate limiter - Limit: ${properties.rateLimit.limit} req/s, " +
            "Window: ${properties.rateLimit.windowSeconds}s, Enabled: ${properties.rateLimit.enabled}"
        }
        return RateLimiter(properties)
    }

    @Bean
    fun collectorClient(properties: MonitoringProperties): CollectorClient {
        logger.info { "Initializing collector client for service: ${properties.service.name}" }
        return CollectorClient(properties)
    }

    @Bean
    fun apiTrackingFilter(
        properties: MonitoringProperties,
        collectorClient: CollectorClient,
        rateLimiter: RateLimiter
    ): ApiTrackingFilter {
        logger.info { "Initializing API tracking filter" }
        return ApiTrackingFilter(properties, collectorClient, rateLimiter)
    }
}
