package com.leapfinance.infopulse.collector.config

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.MongoTransactionManager
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Configuration properties for dual MongoDB setup.
 * Supports MongoDB Atlas connection strings.
 */
@ConfigurationProperties(prefix = "mongodb")
data class MongoDbProperties(
    /** MongoDB Atlas connection URI (or local MongoDB URI) */
    val uri: String = "mongodb://localhost:27017",
    
    /** Primary database name for API logs */
    val logsDatabase: String = "logs_db",
    
    /** Secondary database name for metadata */
    val metaDatabase: String = "meta_db",
    
    /** Connection pool settings */
    val pool: PoolSettings = PoolSettings()
)

data class PoolSettings(
    val minSize: Int = 5,
    val maxSize: Int = 50,
    val maxWaitTimeMs: Long = 10000,
    val maxConnectionIdleTimeMs: Long = 60000
)

/**
 * Dual MongoDB Configuration.
 * 
 * This configuration creates two separate MongoTemplate beans that connect
 * to the same MongoDB cluster (Atlas or local) but use different logical databases.
 * 
 * Architecture:
 * - logsMongoTemplate -> logs_db (high-volume API logs, rate limit events)
 * - metaMongoTemplate -> meta_db (users, incidents, alerts, config)
 */
@Configuration
@EnableConfigurationProperties(MongoDbProperties::class)
class MongoConfig(
    private val properties: MongoDbProperties
) {

    /**
     * Shared MongoDB client connecting to the Atlas cluster.
     * Uses connection pooling for efficiency.
     */
    @Bean
    fun mongoClient(): MongoClient {
        logger.info { "Initializing MongoDB client with URI: ${maskUri(properties.uri)}" }
        
        val connectionString = ConnectionString(properties.uri)
        
        val settings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .applyToConnectionPoolSettings { builder ->
                builder
                    .minSize(properties.pool.minSize)
                    .maxSize(properties.pool.maxSize)
                    .maxWaitTime(properties.pool.maxWaitTimeMs, TimeUnit.MILLISECONDS)
                    .maxConnectionIdleTime(properties.pool.maxConnectionIdleTimeMs, TimeUnit.MILLISECONDS)
            }
            .applyToSocketSettings { builder ->
                builder
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
            }
            .build()
        
        return MongoClients.create(settings)
    }

    // ==================== LOGS DATABASE (Primary) ====================

    /**
     * Database factory for logs_db.
     */
    @Bean
    @Primary
    @Qualifier("logsDatabaseFactory")
    fun logsDatabaseFactory(mongoClient: MongoClient): MongoDatabaseFactory {
        logger.info { "Creating logs database factory for: ${properties.logsDatabase}" }
        return SimpleMongoClientDatabaseFactory(mongoClient, properties.logsDatabase)
    }

    /**
     * MongoTemplate for logs_db - stores API logs and rate limit events.
     * This is the PRIMARY template used for high-volume writes.
     */
    @Bean
    @Primary
    @Qualifier("logsMongoTemplate")
    fun logsMongoTemplate(
        @Qualifier("logsDatabaseFactory") factory: MongoDatabaseFactory
    ): MongoTemplate {
        val converter = createConverter(factory)
        logger.info { "Created logsMongoTemplate for database: ${properties.logsDatabase}" }
        return MongoTemplate(factory, converter)
    }

    /**
     * Transaction manager for logs_db.
     * Note: Transactions require MongoDB replica set (Atlas supports this by default).
     */
    @Bean
    @Primary
    @Qualifier("logsTransactionManager")
    fun logsTransactionManager(
        @Qualifier("logsDatabaseFactory") factory: MongoDatabaseFactory
    ): MongoTransactionManager {
        return MongoTransactionManager(factory)
    }

    // ==================== METADATA DATABASE (Secondary) ====================

    /**
     * Database factory for meta_db.
     */
    @Bean
    @Qualifier("metaDatabaseFactory")
    fun metaDatabaseFactory(mongoClient: MongoClient): MongoDatabaseFactory {
        logger.info { "Creating meta database factory for: ${properties.metaDatabase}" }
        return SimpleMongoClientDatabaseFactory(mongoClient, properties.metaDatabase)
    }

    /**
     * MongoTemplate for meta_db - stores users, incidents, alerts, configs.
     * This template is used for metadata and requires strong consistency.
     */
    @Bean
    @Qualifier("metaMongoTemplate")
    fun metaMongoTemplate(
        @Qualifier("metaDatabaseFactory") factory: MongoDatabaseFactory
    ): MongoTemplate {
        val converter = createConverter(factory)
        logger.info { "Created metaMongoTemplate for database: ${properties.metaDatabase}" }
        return MongoTemplate(factory, converter)
    }

    /**
     * Transaction manager for meta_db.
     */
    @Bean
    @Qualifier("metaTransactionManager")
    fun metaTransactionManager(
        @Qualifier("metaDatabaseFactory") factory: MongoDatabaseFactory
    ): MongoTransactionManager {
        return MongoTransactionManager(factory)
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a MappingMongoConverter without _class field in documents.
     */
    private fun createConverter(factory: MongoDatabaseFactory): MappingMongoConverter {
        val dbRefResolver = DefaultDbRefResolver(factory)
        val mappingContext = MongoMappingContext()
        mappingContext.isAutoIndexCreation = true
        
        val converter = MappingMongoConverter(dbRefResolver, mappingContext)
        // Remove _class field from documents for cleaner storage
        converter.setTypeMapper(DefaultMongoTypeMapper(null))
        converter.afterPropertiesSet()
        
        return converter
    }

    /**
     * Mask sensitive parts of the MongoDB URI for logging.
     */
    private fun maskUri(uri: String): String {
        return uri.replace(Regex("://[^:]+:[^@]+@"), "://****:****@")
    }
}
