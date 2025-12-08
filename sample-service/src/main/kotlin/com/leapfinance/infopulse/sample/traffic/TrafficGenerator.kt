package com.leapfinance.infopulse.sample.traffic

import mu.KotlinLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 * Automatic traffic generator that simulates realistic API usage patterns.
 * Generates varied traffic including:
 * - Normal successful requests
 * - Slow requests
 * - Error requests
 * - Burst traffic patterns
 */
@Component
@EnableScheduling
class TrafficGenerator {
    
    private val webClient = WebClient.builder()
        .baseUrl("http://localhost:8081")
        .build()
    
    private var isRunning = false
    
    companion object {
        // Traffic patterns configuration
        private val NORMAL_ENDPOINTS = listOf(
            "/api/v1/users" to "GET",
            "/api/v1/users/1" to "GET",
            "/api/v1/users/2" to "GET",
            "/api/v1/orders" to "GET",
            "/api/v1/orders/1" to "GET",
            "/api/v1/products" to "GET",
            "/api/v1/products?category=Electronics" to "GET",
            "/api/v1/health" to "GET",
            "/api/v1/load-test" to "GET"
        )
        
        private val SLOW_ENDPOINTS = listOf(
            "/api/v1/slow-endpoint" to "GET"
        )
        
        private val ERROR_ENDPOINTS = listOf(
            "/api/v1/error-endpoint" to "GET",  // 50% failure rate
            "/api/v1/always-error" to "GET"     // Always fails
        )
    }
    
    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        logger.info { "🚀 Traffic Generator initialized - will start generating traffic..." }
        isRunning = true
        
        // Initial burst of traffic on startup
        Thread {
            Thread.sleep(5000) // Wait 5 seconds for everything to be ready
            logger.info { "📊 Starting initial traffic burst..." }
            generateInitialBurst()
        }.start()
    }
    
    /**
     * Generate normal traffic every 2-5 seconds
     */
    @Scheduled(fixedDelay = 3000, initialDelay = 10000)
    fun generateNormalTraffic() {
        if (!isRunning) return
        
        try {
            // Generate 1-3 normal requests
            val requestCount = Random.nextInt(1, 4)
            repeat(requestCount) {
                val (endpoint, method) = NORMAL_ENDPOINTS.random()
                makeRequest(endpoint, method)
                Thread.sleep(Random.nextLong(100, 500))
            }
        } catch (e: Exception) {
            logger.debug { "Traffic generation error (expected): ${e.message}" }
        }
    }
    
    /**
     * Generate slow endpoint traffic every 10 seconds (more frequent)
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 8000)
    fun generateSlowTraffic() {
        if (!isRunning) return
        
        try {
            // Generate 1-2 slow requests
            val requestCount = Random.nextInt(1, 3)
            repeat(requestCount) {
                val (endpoint, method) = SLOW_ENDPOINTS.random()
                logger.debug { "🐢 Generating slow request to $endpoint" }
                makeRequest(endpoint, method)
            }
        } catch (e: Exception) {
            logger.debug { "Slow traffic generation error: ${e.message}" }
        }
    }
    
    /**
     * Generate error traffic every 5 seconds (MUCH more frequent for visible errors)
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    fun generateErrorTraffic() {
        if (!isRunning) return
        
        try {
            // Generate 2-5 error requests each time
            val errorCount = Random.nextInt(2, 6)
            repeat(errorCount) {
                val (endpoint, method) = ERROR_ENDPOINTS.random()
                logger.debug { "💥 Generating error request to $endpoint" }
                makeRequest(endpoint, method)
                Thread.sleep(Random.nextLong(100, 300))
            }
        } catch (e: Exception) {
            // Expected - these endpoints fail on purpose
        }
    }
    
    /**
     * Generate additional random errors every 8 seconds
     */
    @Scheduled(fixedDelay = 8000, initialDelay = 12000)
    fun generateRandomErrors() {
        if (!isRunning) return
        
        try {
            // Always hit the always-error endpoint multiple times
            repeat(Random.nextInt(3, 7)) {
                makeRequest("/api/v1/always-error", "GET")
                Thread.sleep(Random.nextLong(50, 150))
            }
        } catch (e: Exception) {
            // Expected
        }
    }
    
    /**
     * Generate burst traffic every 30-60 seconds (simulates traffic spikes)
     */
    @Scheduled(fixedDelay = 45000, initialDelay = 30000)
    fun generateBurstTraffic() {
        if (!isRunning) return
        
        logger.info { "⚡ Generating traffic burst..." }
        
        Thread {
            try {
                // Burst of 10-20 requests in quick succession
                val burstSize = Random.nextInt(10, 21)
                repeat(burstSize) {
                    val (endpoint, method) = NORMAL_ENDPOINTS.random()
                    makeRequest(endpoint, method)
                    Thread.sleep(Random.nextLong(50, 200))
                }
                logger.info { "⚡ Traffic burst complete: $burstSize requests" }
            } catch (e: Exception) {
                logger.debug { "Burst traffic error: ${e.message}" }
            }
        }.start()
    }
    
    /**
     * Generate mixed traffic pattern every minute
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 45000)
    fun generateMixedPattern() {
        if (!isRunning) return
        
        logger.info { "🔄 Generating mixed traffic pattern..." }
        
        Thread {
            try {
                // Mix of all types
                repeat(5) {
                    // Normal requests
                    val (normalEndpoint, normalMethod) = NORMAL_ENDPOINTS.random()
                    makeRequest(normalEndpoint, normalMethod)
                    Thread.sleep(200)
                    
                    // Occasional slow request
                    if (Random.nextInt(100) < 20) { // 20% chance
                        val (slowEndpoint, slowMethod) = SLOW_ENDPOINTS.random()
                        makeRequest(slowEndpoint, slowMethod)
                    }
                    
                    // Occasional error request
                    if (Random.nextInt(100) < 30) { // 30% chance
                        val (errorEndpoint, errorMethod) = ERROR_ENDPOINTS.random()
                        makeRequest(errorEndpoint, errorMethod)
                    }
                    
                    Thread.sleep(Random.nextLong(500, 1500))
                }
            } catch (e: Exception) {
                logger.debug { "Mixed pattern error: ${e.message}" }
            }
        }.start()
    }
    
    /**
     * Create orders with POST requests every 25 seconds
     */
    @Scheduled(fixedDelay = 25000, initialDelay = 20000)
    fun generateOrderCreation() {
        if (!isRunning) return
        
        try {
            val orderJson = """
                {
                    "customerId": "CUST-${Random.nextInt(1000)}",
                    "items": [
                        {"productId": "PROD-${Random.nextInt(100)}", "quantity": ${Random.nextInt(1, 5)}, "price": ${Random.nextDouble(10.0, 100.0)}}
                    ]
                }
            """.trimIndent()
            
            webClient.post()
                .uri("/api/v1/orders")
                .header("Content-Type", "application/json")
                .bodyValue(orderJson)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                    { logger.debug { "📦 Order created successfully" } },
                    { e -> logger.debug { "Order creation handled: ${e.message}" } }
                )
        } catch (e: Exception) {
            logger.debug { "Order creation error: ${e.message}" }
        }
    }
    
    /**
     * Initial burst of traffic when the application starts
     */
    private fun generateInitialBurst() {
        try {
            logger.info { "🎬 Generating initial traffic burst (50 requests)..." }
            
            // Generate 50 varied requests to seed the dashboard
            repeat(50) { i ->
                when {
                    i % 10 == 0 -> {
                        // Every 10th request is slow
                        val (endpoint, method) = SLOW_ENDPOINTS.random()
                        makeRequest(endpoint, method)
                    }
                    i % 5 == 0 -> {
                        // Every 5th request might error
                        val (endpoint, method) = ERROR_ENDPOINTS.random()
                        makeRequest(endpoint, method)
                    }
                    else -> {
                        // Normal traffic
                        val (endpoint, method) = NORMAL_ENDPOINTS.random()
                        makeRequest(endpoint, method)
                    }
                }
                Thread.sleep(Random.nextLong(100, 300))
            }
            
            logger.info { "✅ Initial traffic burst complete!" }
        } catch (e: Exception) {
            logger.error(e) { "Error in initial burst" }
        }
    }
    
    private fun makeRequest(endpoint: String, method: String) {
        try {
            when (method) {
                "GET" -> {
                    webClient.get()
                        .uri(endpoint)
                        .retrieve()
                        .toBodilessEntity()
                        .subscribe(
                            { /* Success */ },
                            { e -> 
                                // Only log unexpected errors
                                if (e !is WebClientResponseException) {
                                    logger.debug { "Request to $endpoint: ${e.message}" }
                                }
                            }
                        )
                }
                "POST" -> {
                    webClient.post()
                        .uri(endpoint)
                        .retrieve()
                        .toBodilessEntity()
                        .subscribe(
                            { /* Success */ },
                            { e -> logger.debug { "POST to $endpoint: ${e.message}" } }
                        )
                }
            }
        } catch (e: Exception) {
            logger.debug { "Request error: ${e.message}" }
        }
    }
}
