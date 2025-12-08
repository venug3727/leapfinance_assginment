package com.leapfinance.infopulse.collector.repository.logs

import com.leapfinance.infopulse.collector.entity.logs.ApiLogEntity
import com.leapfinance.infopulse.collector.entity.logs.RateLimitEventEntity
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Repository for API logs stored in logs_db.
 */
@Repository
class ApiLogRepository(
    @Qualifier("logsMongoTemplate")
    private val mongoTemplate: MongoTemplate
) {
    
    fun save(entity: ApiLogEntity): ApiLogEntity {
        return mongoTemplate.save(entity)
    }
    
    fun saveAll(entities: List<ApiLogEntity>): List<ApiLogEntity> {
        return entities.map { mongoTemplate.save(it) }
    }
    
    fun findById(id: String): ApiLogEntity? {
        return mongoTemplate.findById(id, ApiLogEntity::class.java)
    }
    
    fun findAll(pageable: Pageable): Page<ApiLogEntity> {
        val query = Query().with(pageable)
        val total = mongoTemplate.count(Query(), ApiLogEntity::class.java)
        val results = mongoTemplate.find(query, ApiLogEntity::class.java)
        return PageImpl(results, pageable, total)
    }
    
    /**
     * Find logs with multiple filter criteria.
     */
    fun findWithFilters(
        serviceName: String? = null,
        endpoint: String? = null,
        method: String? = null,
        statusCode: Int? = null,
        minLatency: Long? = null,
        maxLatency: Long? = null,
        startTime: Instant? = null,
        endTime: Instant? = null,
        isSlow: Boolean? = null,
        isBroken: Boolean? = null,
        pageable: Pageable
    ): Page<ApiLogEntity> {
        val criteria = mutableListOf<Criteria>()
        
        serviceName?.let { criteria.add(Criteria.where("serviceName").`is`(it)) }
        endpoint?.let { criteria.add(Criteria.where("endpoint").regex(it, "i")) }
        method?.let { criteria.add(Criteria.where("method").`is`(it)) }
        statusCode?.let { criteria.add(Criteria.where("statusCode").`is`(it)) }
        minLatency?.let { criteria.add(Criteria.where("latency").gte(it)) }
        maxLatency?.let { criteria.add(Criteria.where("latency").lte(it)) }
        startTime?.let { criteria.add(Criteria.where("timestamp").gte(it)) }
        endTime?.let { criteria.add(Criteria.where("timestamp").lte(it)) }
        isSlow?.let { criteria.add(Criteria.where("isSlow").`is`(it)) }
        isBroken?.let { criteria.add(Criteria.where("isBroken").`is`(it)) }
        
        val query = if (criteria.isNotEmpty()) {
            Query(Criteria().andOperator(*criteria.toTypedArray()))
        } else {
            Query()
        }
        
        val total = mongoTemplate.count(query, ApiLogEntity::class.java)
        val results = mongoTemplate.find(query.with(pageable), ApiLogEntity::class.java)
        
        return PageImpl(results, pageable, total)
    }
    
    /**
     * Count logs by service.
     */
    fun countByService(serviceName: String): Long {
        val query = Query(Criteria.where("serviceName").`is`(serviceName))
        return mongoTemplate.count(query, ApiLogEntity::class.java)
    }
    
    /**
     * Get distinct service names.
     */
    fun findDistinctServices(): List<String> {
        return mongoTemplate.findDistinct(
            Query(),
            "serviceName",
            ApiLogEntity::class.java,
            String::class.java
        )
    }
    
    /**
     * Get distinct endpoints for a service.
     */
    fun findDistinctEndpoints(serviceName: String? = null): List<String> {
        val query = serviceName?.let { 
            Query(Criteria.where("serviceName").`is`(it)) 
        } ?: Query()
        
        return mongoTemplate.findDistinct(
            query,
            "endpoint",
            ApiLogEntity::class.java,
            String::class.java
        )
    }
    
    /**
     * Count slow APIs (latency > 500ms).
     */
    fun countSlowApis(startTime: Instant? = null, endTime: Instant? = null): Long {
        val criteria = mutableListOf(Criteria.where("isSlow").`is`(true))
        startTime?.let { criteria.add(Criteria.where("timestamp").gte(it)) }
        endTime?.let { criteria.add(Criteria.where("timestamp").lte(it)) }
        
        val query = Query(Criteria().andOperator(*criteria.toTypedArray()))
        return mongoTemplate.count(query, ApiLogEntity::class.java)
    }
    
    /**
     * Count broken APIs (5xx status).
     */
    fun countBrokenApis(startTime: Instant? = null, endTime: Instant? = null): Long {
        val criteria = mutableListOf(Criteria.where("isBroken").`is`(true))
        startTime?.let { criteria.add(Criteria.where("timestamp").gte(it)) }
        endTime?.let { criteria.add(Criteria.where("timestamp").lte(it)) }
        
        val query = Query(Criteria().andOperator(*criteria.toTypedArray()))
        return mongoTemplate.count(query, ApiLogEntity::class.java)
    }
    
    /**
     * Get average latency by endpoint.
     */
    fun getAverageLatencyByEndpoint(
        serviceName: String? = null,
        startTime: Instant? = null,
        endTime: Instant? = null,
        limit: Int = 10
    ): List<EndpointLatencyStats> {
        val matchCriteria = mutableListOf<Criteria>()
        serviceName?.let { matchCriteria.add(Criteria.where("serviceName").`is`(it)) }
        startTime?.let { matchCriteria.add(Criteria.where("timestamp").gte(it)) }
        endTime?.let { matchCriteria.add(Criteria.where("timestamp").lte(it)) }
        
        val aggregation = org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
            if (matchCriteria.isNotEmpty()) {
                org.springframework.data.mongodb.core.aggregation.Aggregation.match(
                    Criteria().andOperator(*matchCriteria.toTypedArray())
                )
            } else {
                org.springframework.data.mongodb.core.aggregation.Aggregation.match(Criteria())
            },
            org.springframework.data.mongodb.core.aggregation.Aggregation.group("endpoint", "serviceName")
                .avg("latency").`as`("avgLatency")
                .count().`as`("requestCount")
                .max("latency").`as`("maxLatency"),
            org.springframework.data.mongodb.core.aggregation.Aggregation.sort(
                org.springframework.data.domain.Sort.Direction.DESC, "avgLatency"
            ),
            org.springframework.data.mongodb.core.aggregation.Aggregation.limit(limit.toLong())
        )
        
        val results = mongoTemplate.aggregate(
            aggregation,
            "api_logs",
            EndpointLatencyStatsRaw::class.java
        )
        
        return results.mappedResults.map { raw ->
            EndpointLatencyStats(
                endpoint = raw.id?.endpoint ?: "unknown",
                serviceName = raw.id?.serviceName ?: "unknown",
                avgLatency = raw.avgLatency,
                maxLatency = raw.maxLatency,
                requestCount = raw.requestCount
            )
        }
    }
    
    /**
     * Get error rate by endpoint.
     */
    fun getErrorRateByEndpoint(
        startTime: Instant? = null,
        endTime: Instant? = null
    ): List<EndpointErrorRate> {
        val matchCriteria = mutableListOf<Criteria>()
        startTime?.let { matchCriteria.add(Criteria.where("timestamp").gte(it)) }
        endTime?.let { matchCriteria.add(Criteria.where("timestamp").lte(it)) }
        
        val aggregation = org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
            if (matchCriteria.isNotEmpty()) {
                org.springframework.data.mongodb.core.aggregation.Aggregation.match(
                    Criteria().andOperator(*matchCriteria.toTypedArray())
                )
            } else {
                org.springframework.data.mongodb.core.aggregation.Aggregation.match(Criteria())
            },
            org.springframework.data.mongodb.core.aggregation.Aggregation.group("endpoint", "serviceName")
                .count().`as`("totalCount")
                .sum(
                    org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                        .`when`(Criteria.where("isBroken").`is`(true))
                        .then(1)
                        .otherwise(0)
                ).`as`("errorCount"),
            org.springframework.data.mongodb.core.aggregation.Aggregation.project()
                .and("_id.endpoint").`as`("endpoint")
                .and("_id.serviceName").`as`("serviceName")
                .and("totalCount").`as`("totalCount")
                .and("errorCount").`as`("errorCount")
                .andExpression("errorCount * 100.0 / totalCount").`as`("errorRate")
        )
        
        return mongoTemplate.aggregate(
            aggregation,
            "api_logs",
            EndpointErrorRate::class.java
        ).mappedResults
    }
    
    /**
     * Get comprehensive endpoint statistics for health score calculation.
     */
    fun getEndpointStatistics(
        startTime: Instant? = null,
        endTime: Instant? = null
    ): List<com.leapfinance.infopulse.collector.service.EndpointStatistics> {
        val matchCriteria = mutableListOf<Criteria>()
        startTime?.let { matchCriteria.add(Criteria.where("timestamp").gte(it)) }
        endTime?.let { matchCriteria.add(Criteria.where("timestamp").lte(it)) }
        
        val aggregation = org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
            if (matchCriteria.isNotEmpty()) {
                org.springframework.data.mongodb.core.aggregation.Aggregation.match(
                    Criteria().andOperator(*matchCriteria.toTypedArray())
                )
            } else {
                org.springframework.data.mongodb.core.aggregation.Aggregation.match(Criteria())
            },
            org.springframework.data.mongodb.core.aggregation.Aggregation.group("endpoint", "serviceName", "method")
                .avg("latency").`as`("avgLatency")
                .max("latency").`as`("maxLatency")
                .min("latency").`as`("minLatency")
                .count().`as`("requestCount")
                .sum(
                    org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                        .`when`(Criteria.where("isBroken").`is`(true))
                        .then(1)
                        .otherwise(0)
                ).`as`("errorCount")
                .sum(
                    org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                        .`when`(Criteria.where("isBroken").`is`(false))
                        .then(1)
                        .otherwise(0)
                ).`as`("successCount")
                .push("latency").`as`("latencies")
                .max("timestamp").`as`("lastRequestAt")
        )
        
        val results = mongoTemplate.aggregate(
            aggregation,
            "api_logs",
            EndpointStatisticsRaw::class.java
        )
        
        return results.mappedResults.map { raw ->
            val sortedLatencies = raw.latencies.sorted()
            val p95Index = ((sortedLatencies.size * 0.95).toInt()).coerceIn(0, sortedLatencies.size - 1)
            val p99Index = ((sortedLatencies.size * 0.99).toInt()).coerceIn(0, sortedLatencies.size - 1)
            
            com.leapfinance.infopulse.collector.service.EndpointStatistics(
                serviceName = raw.id?.serviceName ?: "unknown",
                endpoint = raw.id?.endpoint ?: "unknown",
                method = raw.id?.method ?: "GET",
                avgLatency = raw.avgLatency,
                p95Latency = if (sortedLatencies.isNotEmpty()) sortedLatencies[p95Index] else 0L,
                p99Latency = if (sortedLatencies.isNotEmpty()) sortedLatencies[p99Index] else 0L,
                errorRate = if (raw.requestCount > 0) raw.errorCount.toDouble() / raw.requestCount else 0.0,
                successRate = if (raw.requestCount > 0) raw.successCount.toDouble() / raw.requestCount else 1.0,
                requestCount = raw.requestCount,
                lastRequestAt = raw.lastRequestAt ?: Instant.now()
            )
        }
    }
    
    /**
     * Get statistics for a specific endpoint.
     */
    fun getEndpointStats(
        serviceName: String,
        endpoint: String,
        startTime: Instant? = null,
        endTime: Instant? = null
    ): com.leapfinance.infopulse.collector.service.EndpointStatistics? {
        return getEndpointStatistics(startTime, endTime)
            .find { it.serviceName == serviceName && it.endpoint == endpoint }
    }
}

data class EndpointLatencyStats(
    val endpoint: String,
    val serviceName: String,
    val avgLatency: Double,
    val maxLatency: Long,
    val requestCount: Long
)

data class EndpointLatencyStatsRaw(
    val id: EndpointId? = null,
    val avgLatency: Double = 0.0,
    val maxLatency: Long = 0,
    val requestCount: Long = 0
)

data class EndpointId(
    val endpoint: String? = null,
    val serviceName: String? = null
)

data class EndpointErrorRate(
    val endpoint: String = "",
    val serviceName: String = "",
    val totalCount: Long = 0,
    val errorCount: Long = 0,
    val errorRate: Double = 0.0
)

data class EndpointStatisticsRaw(
    val id: EndpointGroupId? = null,
    val avgLatency: Double = 0.0,
    val maxLatency: Long = 0,
    val minLatency: Long = 0,
    val requestCount: Long = 0,
    val errorCount: Long = 0,
    val successCount: Long = 0,
    val latencies: List<Long> = emptyList(),
    val lastRequestAt: java.time.Instant? = null
)

data class EndpointGroupId(
    val endpoint: String? = null,
    val serviceName: String? = null,
    val method: String? = null
)

/**
 * Repository for rate limit events stored in logs_db.
 */
@Repository
class RateLimitEventRepository(
    @Qualifier("logsMongoTemplate")
    private val mongoTemplate: MongoTemplate
) {
    
    fun save(entity: RateLimitEventEntity): RateLimitEventEntity {
        return mongoTemplate.save(entity)
    }
    
    fun saveAll(entities: List<RateLimitEventEntity>): List<RateLimitEventEntity> {
        return entities.map { mongoTemplate.save(it) }
    }
    
    fun findAll(pageable: Pageable): Page<RateLimitEventEntity> {
        val query = Query().with(pageable)
        val total = mongoTemplate.count(Query(), RateLimitEventEntity::class.java)
        val results = mongoTemplate.find(query, RateLimitEventEntity::class.java)
        return PageImpl(results, pageable, total)
    }
    
    fun findByServiceName(
        serviceName: String,
        startTime: Instant? = null,
        endTime: Instant? = null,
        pageable: Pageable
    ): Page<RateLimitEventEntity> {
        val criteria = mutableListOf(Criteria.where("serviceName").`is`(serviceName))
        startTime?.let { criteria.add(Criteria.where("timestamp").gte(it)) }
        endTime?.let { criteria.add(Criteria.where("timestamp").lte(it)) }
        
        val query = Query(Criteria().andOperator(*criteria.toTypedArray()))
        val total = mongoTemplate.count(query, RateLimitEventEntity::class.java)
        val results = mongoTemplate.find(query.with(pageable), RateLimitEventEntity::class.java)
        
        return PageImpl(results, pageable, total)
    }
    
    fun countByServiceName(
        serviceName: String? = null,
        startTime: Instant? = null,
        endTime: Instant? = null
    ): Long {
        val criteria = mutableListOf<Criteria>()
        serviceName?.let { criteria.add(Criteria.where("serviceName").`is`(it)) }
        startTime?.let { criteria.add(Criteria.where("timestamp").gte(it)) }
        endTime?.let { criteria.add(Criteria.where("timestamp").lte(it)) }
        
        val query = if (criteria.isNotEmpty()) {
            Query(Criteria().andOperator(*criteria.toTypedArray()))
        } else {
            Query()
        }
        
        return mongoTemplate.count(query, RateLimitEventEntity::class.java)
    }
}
