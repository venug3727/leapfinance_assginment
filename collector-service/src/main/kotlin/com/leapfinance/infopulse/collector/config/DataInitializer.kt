package com.leapfinance.infopulse.collector.config

import com.leapfinance.infopulse.collector.entity.meta.UserEntity
import com.leapfinance.infopulse.collector.entity.meta.UserRole
import com.leapfinance.infopulse.collector.repository.meta.UserRepository
import mu.KotlinLogging
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder

private val logger = KotlinLogging.logger {}

@Configuration
class DataInitializer {

    @Bean
    fun initializeData(
        userRepository: UserRepository,
        passwordEncoder: PasswordEncoder
    ): CommandLineRunner = CommandLineRunner {
        // Check if admin user exists
        val existingAdmin = userRepository.findByUsername("admin")
        
        if (existingAdmin == null) {
            logger.info { "Creating default admin user..." }
            
            val adminUser = UserEntity(
                username = "admin",
                email = "admin@infopulse.com",
                password = passwordEncoder.encode("admin123"),
                role = UserRole.ADMIN
            )
            
            userRepository.save(adminUser)
            logger.info { "Default admin user created successfully" }
            logger.info { "  Username: admin" }
            logger.info { "  Password: admin123" }
        } else {
            logger.info { "Admin user already exists, skipping initialization" }
        }
    }
}
