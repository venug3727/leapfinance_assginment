package com.leapfinance.infopulse.sample.controller

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 * Sample controller with various endpoints to demonstrate monitoring capabilities.
 */
@RestController
@RequestMapping("/api/v1")
class SampleController {

    // ==================== Order Endpoints ====================
    
    @GetMapping("/orders")
    fun getOrders(): ResponseEntity<List<Order>> {
        simulateLatency(50, 200)
        val orders = (1..10).map { 
            Order(
                id = it.toString(),
                customerId = "CUST-${Random.nextInt(100)}",
                total = Random.nextDouble(10.0, 500.0),
                status = listOf("PENDING", "PROCESSING", "SHIPPED", "DELIVERED").random()
            )
        }
        return ResponseEntity.ok(orders)
    }
    
    @GetMapping("/orders/{id}")
    fun getOrderById(@PathVariable id: String): ResponseEntity<Order> {
        simulateLatency(30, 100)
        return ResponseEntity.ok(
            Order(
                id = id,
                customerId = "CUST-${Random.nextInt(100)}",
                total = Random.nextDouble(10.0, 500.0),
                status = "PROCESSING"
            )
        )
    }
    
    @PostMapping("/orders")
    fun createOrder(@RequestBody request: CreateOrderRequest): ResponseEntity<Order> {
        simulateLatency(100, 300)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            Order(
                id = Random.nextInt(1000, 9999).toString(),
                customerId = request.customerId,
                total = request.items.sumOf { it.price * it.quantity },
                status = "PENDING"
            )
        )
    }
    
    // ==================== Slow Endpoint (for testing) ====================
    
    @GetMapping("/slow-endpoint")
    fun slowEndpoint(): ResponseEntity<Map<String, String>> {
        // Deliberately slow - will trigger slow API alerts
        simulateLatency(600, 1200)
        return ResponseEntity.ok(mapOf("message" to "This endpoint is intentionally slow"))
    }
    
    // ==================== Error Endpoint (for testing) ====================
    
    @GetMapping("/error-endpoint")
    fun errorEndpoint(): ResponseEntity<Map<String, String>> {
        simulateLatency(50, 100)
        // Randomly throw errors to simulate broken API
        if (Random.nextBoolean()) {
            throw RuntimeException("Simulated server error")
        }
        return ResponseEntity.ok(mapOf("message" to "Success"))
    }
    
    @GetMapping("/always-error")
    fun alwaysError(): ResponseEntity<Map<String, String>> {
        throw RuntimeException("This endpoint always fails")
    }
    
    // ==================== User Endpoints ====================
    
    @GetMapping("/users")
    fun getUsers(): ResponseEntity<List<User>> {
        simulateLatency(40, 150)
        val users = (1..5).map {
            User(
                id = it.toString(),
                name = "User $it",
                email = "user$it@example.com"
            )
        }
        return ResponseEntity.ok(users)
    }
    
    @GetMapping("/users/{id}")
    fun getUserById(@PathVariable id: String): ResponseEntity<User> {
        simulateLatency(20, 80)
        return ResponseEntity.ok(
            User(
                id = id,
                name = "User $id",
                email = "user$id@example.com"
            )
        )
    }
    
    // ==================== Product Endpoints ====================
    
    @GetMapping("/products")
    fun getProducts(
        @RequestParam(required = false) category: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Map<String, Any>> {
        simulateLatency(80, 250)
        val products = (1..size).map {
            Product(
                id = (page * size + it).toString(),
                name = "Product ${page * size + it}",
                price = Random.nextDouble(5.0, 200.0),
                category = category ?: listOf("Electronics", "Clothing", "Food", "Books").random()
            )
        }
        return ResponseEntity.ok(mapOf(
            "content" to products,
            "page" to page,
            "size" to size,
            "total" to 100
        ))
    }
    
    @PostMapping("/products")
    fun createProduct(@RequestBody product: Product): ResponseEntity<Product> {
        simulateLatency(100, 200)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            product.copy(id = Random.nextInt(1000, 9999).toString())
        )
    }
    
    // ==================== Health Check ====================
    
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "service" to "sample-service"
        ))
    }
    
    // ==================== Load Test Endpoint ====================
    
    @GetMapping("/load-test")
    fun loadTest(): ResponseEntity<Map<String, Any>> {
        simulateLatency(10, 50)
        return ResponseEntity.ok(mapOf(
            "timestamp" to System.currentTimeMillis(),
            "random" to Random.nextInt(1000)
        ))
    }
    
    // ==================== Helper Functions ====================
    
    private fun simulateLatency(minMs: Int, maxMs: Int) {
        Thread.sleep(Random.nextLong(minMs.toLong(), maxMs.toLong()))
    }
    
    // ==================== Exception Handler ====================
    
    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(e: RuntimeException): ResponseEntity<Map<String, String>> {
        logger.error(e) { "Error in sample service" }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            mapOf("error" to (e.message ?: "Unknown error"))
        )
    }
}

// ==================== Data Classes ====================

data class Order(
    val id: String,
    val customerId: String,
    val total: Double,
    val status: String
)

data class CreateOrderRequest(
    val customerId: String,
    val items: List<OrderItem>
)

data class OrderItem(
    val productId: String,
    val quantity: Int,
    val price: Double
)

data class User(
    val id: String,
    val name: String,
    val email: String
)

data class Product(
    val id: String? = null,
    val name: String,
    val price: Double,
    val category: String
)
