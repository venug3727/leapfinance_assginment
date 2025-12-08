package com.leapfinance.infopulse.collector.repository.meta

import com.leapfinance.infopulse.collector.entity.meta.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Repository for users stored in meta_db.
 */
@Repository
class UserRepository(
    @Qualifier("metaMongoTemplate")
    private val mongoTemplate: MongoTemplate
) {
    
    fun save(entity: UserEntity): UserEntity {
        return mongoTemplate.save(entity)
    }
    
    fun findById(id: String): UserEntity? {
        return mongoTemplate.findById(id, UserEntity::class.java)
    }
    
    fun findByUsername(username: String): UserEntity? {
        val query = Query(Criteria.where("username").`is`(username))
        return mongoTemplate.findOne(query, UserEntity::class.java)
    }
    
    fun findByEmail(email: String): UserEntity? {
        val query = Query(Criteria.where("email").`is`(email))
        return mongoTemplate.findOne(query, UserEntity::class.java)
    }
    
    fun existsByUsername(username: String): Boolean {
        val query = Query(Criteria.where("username").`is`(username))
        return mongoTemplate.exists(query, UserEntity::class.java)
    }
    
    fun existsByEmail(email: String): Boolean {
        val query = Query(Criteria.where("email").`is`(email))
        return mongoTemplate.exists(query, UserEntity::class.java)
    }
    
    fun findAll(): List<UserEntity> {
        return mongoTemplate.findAll(UserEntity::class.java)
    }
}

/**
 * Repository for incidents stored in meta_db.
 * Implements optimistic locking for concurrent access.
 */
@Repository
class IncidentRepository(
    @Qualifier("metaMongoTemplate")
    private val mongoTemplate: MongoTemplate
) {
    
    fun save(entity: IncidentEntity): IncidentEntity {
        return mongoTemplate.save(entity)
    }
    
    fun findById(id: String): IncidentEntity? {
        return mongoTemplate.findById(id, IncidentEntity::class.java)
    }
    
    fun findAll(pageable: Pageable): Page<IncidentEntity> {
        val query = Query().with(pageable)
        val total = mongoTemplate.count(Query(), IncidentEntity::class.java)
        val results = mongoTemplate.find(query, IncidentEntity::class.java)
        return PageImpl(results, pageable, total)
    }
    
    fun findByStatus(status: IncidentStatus, pageable: Pageable): Page<IncidentEntity> {
        val query = Query(Criteria.where("status").`is`(status)).with(pageable)
        val total = mongoTemplate.count(
            Query(Criteria.where("status").`is`(status)),
            IncidentEntity::class.java
        )
        val results = mongoTemplate.find(query, IncidentEntity::class.java)
        return PageImpl(results, pageable, total)
    }
    
    fun findByServiceAndEndpoint(serviceName: String, endpoint: String): IncidentEntity? {
        val query = Query(
            Criteria.where("serviceName").`is`(serviceName)
                .and("endpoint").`is`(endpoint)
                .and("status").ne(IncidentStatus.RESOLVED)
        )
        return mongoTemplate.findOne(query, IncidentEntity::class.java)
    }
    
    fun findWithFilters(
        serviceName: String? = null,
        endpoint: String? = null,
        incidentType: IncidentType? = null,
        status: IncidentStatus? = null,
        startTime: Instant? = null,
        endTime: Instant? = null,
        pageable: Pageable
    ): Page<IncidentEntity> {
        val criteria = mutableListOf<Criteria>()
        
        serviceName?.let { criteria.add(Criteria.where("serviceName").`is`(it)) }
        endpoint?.let { criteria.add(Criteria.where("endpoint").regex(it, "i")) }
        incidentType?.let { criteria.add(Criteria.where("incidentType").`is`(it)) }
        status?.let { criteria.add(Criteria.where("status").`is`(it)) }
        startTime?.let { criteria.add(Criteria.where("createdAt").gte(it)) }
        endTime?.let { criteria.add(Criteria.where("createdAt").lte(it)) }
        
        val query = if (criteria.isNotEmpty()) {
            Query(Criteria().andOperator(*criteria.toTypedArray()))
        } else {
            Query()
        }
        
        val total = mongoTemplate.count(query, IncidentEntity::class.java)
        val results = mongoTemplate.find(query.with(pageable), IncidentEntity::class.java)
        
        return PageImpl(results, pageable, total)
    }
    
    /**
     * Resolve an incident with optimistic locking.
     * Throws OptimisticLockingFailureException if the document was modified by another process.
     */
    fun resolveIncident(
        id: String,
        expectedVersion: Long,
        resolvedBy: String,
        resolutionNotes: String?
    ): IncidentEntity {
        val query = Query(
            Criteria.where("_id").`is`(id)
                .and("version").`is`(expectedVersion)
        )
        
        val update = Update()
            .set("status", IncidentStatus.RESOLVED)
            .set("resolvedBy", resolvedBy)
            .set("resolvedAt", Instant.now())
            .set("resolutionNotes", resolutionNotes)
            .set("updatedAt", Instant.now())
            .inc("version", 1)
        
        val result = mongoTemplate.findAndModify(
            query,
            update,
            FindAndModifyOptions.options().returnNew(true),
            IncidentEntity::class.java
        )
        
        return result ?: throw OptimisticLockingFailureException(
            "Incident $id was modified by another process. Please refresh and try again."
        )
    }
    
    /**
     * Increment occurrence count atomically.
     */
    fun incrementOccurrence(id: String): IncidentEntity? {
        val query = Query(Criteria.where("_id").`is`(id))
        val update = Update()
            .inc("occurrenceCount", 1)
            .set("lastSeenAt", Instant.now())
            .set("updatedAt", Instant.now())
        
        return mongoTemplate.findAndModify(
            query,
            update,
            FindAndModifyOptions.options().returnNew(true),
            IncidentEntity::class.java
        )
    }
    
    fun countByStatus(status: IncidentStatus): Long {
        val query = Query(Criteria.where("status").`is`(status))
        return mongoTemplate.count(query, IncidentEntity::class.java)
    }
    
    fun countByType(type: IncidentType): Long {
        val query = Query(Criteria.where("incidentType").`is`(type))
        return mongoTemplate.count(query, IncidentEntity::class.java)
    }
}

/**
 * Repository for alerts stored in meta_db.
 */
@Repository
class AlertRepository(
    @Qualifier("metaMongoTemplate")
    private val mongoTemplate: MongoTemplate
) {
    
    fun save(entity: AlertEntity): AlertEntity {
        return mongoTemplate.save(entity)
    }
    
    fun findById(id: String): AlertEntity? {
        return mongoTemplate.findById(id, AlertEntity::class.java)
    }
    
    fun findAll(pageable: Pageable): Page<AlertEntity> {
        val query = Query().with(pageable)
        val total = mongoTemplate.count(Query(), AlertEntity::class.java)
        val results = mongoTemplate.find(query, AlertEntity::class.java)
        return PageImpl(results, pageable, total)
    }
    
    fun findUnacknowledged(pageable: Pageable): Page<AlertEntity> {
        val query = Query(Criteria.where("acknowledged").`is`(false)).with(pageable)
        val total = mongoTemplate.count(
            Query(Criteria.where("acknowledged").`is`(false)),
            AlertEntity::class.java
        )
        val results = mongoTemplate.find(query, AlertEntity::class.java)
        return PageImpl(results, pageable, total)
    }
    
    fun findByType(alertType: AlertType, pageable: Pageable): Page<AlertEntity> {
        val query = Query(Criteria.where("alertType").`is`(alertType)).with(pageable)
        val total = mongoTemplate.count(
            Query(Criteria.where("alertType").`is`(alertType)),
            AlertEntity::class.java
        )
        val results = mongoTemplate.find(query, AlertEntity::class.java)
        return PageImpl(results, pageable, total)
    }
    
    fun acknowledgeAlert(id: String, acknowledgedBy: String): AlertEntity? {
        val query = Query(Criteria.where("_id").`is`(id))
        val update = Update()
            .set("acknowledged", true)
            .set("acknowledgedBy", acknowledgedBy)
            .set("acknowledgedAt", Instant.now())
        
        return mongoTemplate.findAndModify(
            query,
            update,
            FindAndModifyOptions.options().returnNew(true),
            AlertEntity::class.java
        )
    }
    
    fun countUnacknowledged(): Long {
        val query = Query(Criteria.where("acknowledged").`is`(false))
        return mongoTemplate.count(query, AlertEntity::class.java)
    }
    
    fun countByType(alertType: AlertType): Long {
        val query = Query(Criteria.where("alertType").`is`(alertType))
        return mongoTemplate.count(query, AlertEntity::class.java)
    }
}

/**
 * Repository for rate limiter configs stored in meta_db.
 */
@Repository
class RateLimiterConfigRepository(
    @Qualifier("metaMongoTemplate")
    private val mongoTemplate: MongoTemplate
) {
    
    fun save(entity: RateLimiterConfigEntity): RateLimiterConfigEntity {
        return mongoTemplate.save(entity)
    }
    
    fun findByServiceName(serviceName: String): RateLimiterConfigEntity? {
        val query = Query(Criteria.where("serviceName").`is`(serviceName))
        return mongoTemplate.findOne(query, RateLimiterConfigEntity::class.java)
    }
    
    fun findAll(): List<RateLimiterConfigEntity> {
        return mongoTemplate.findAll(RateLimiterConfigEntity::class.java)
    }
    
    fun deleteByServiceName(serviceName: String) {
        val query = Query(Criteria.where("serviceName").`is`(serviceName))
        mongoTemplate.remove(query, RateLimiterConfigEntity::class.java)
    }
}

/**
 * Repository for resolution audit trail stored in meta_db.
 */
@Repository
class ResolutionAuditRepository(
    @Qualifier("metaMongoTemplate")
    private val mongoTemplate: MongoTemplate
) {
    
    fun save(entity: ResolutionAuditEntity): ResolutionAuditEntity {
        return mongoTemplate.save(entity)
    }
    
    fun findByIncidentId(incidentId: String): List<ResolutionAuditEntity> {
        val query = Query(Criteria.where("incidentId").`is`(incidentId))
            .with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC,
                "changedAt"
            ))
        return mongoTemplate.find(query, ResolutionAuditEntity::class.java)
    }
}
