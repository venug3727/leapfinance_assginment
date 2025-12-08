package com.leapfinance.infopulse.collector.config

import com.leapfinance.infopulse.collector.security.AuthenticationFilter
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(SecurityProperties::class)
class SecurityConfig(
    private val authenticationFilter: AuthenticationFilter,
    @org.springframework.beans.factory.annotation.Value("\${cors.allowed-origins:http://localhost:3000}")
    private val corsAllowedOrigins: String
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    
                    // WebSocket endpoints - allow public access
                    .requestMatchers("/ws/**").permitAll()
                    
                    // Health Score APIs - public for dashboard
                    .requestMatchers("/api/v1/health-score/**").hasAnyRole("ADMIN", "DEVELOPER", "VIEWER")
                    
                    // Log ingestion - service API key required
                    .requestMatchers("/api/v1/logs/**").hasAnyRole("SERVICE", "ADMIN")
                    
                    // Dashboard APIs - JWT required
                    .requestMatchers(HttpMethod.GET, "/api/v1/dashboard/**").hasAnyRole("ADMIN", "DEVELOPER", "VIEWER")
                    .requestMatchers(HttpMethod.POST, "/api/v1/incidents/*/resolve").hasAnyRole("ADMIN", "DEVELOPER")
                    .requestMatchers("/api/v1/incidents/**").hasAnyRole("ADMIN", "DEVELOPER", "VIEWER")
                    .requestMatchers("/api/v1/alerts/**").hasAnyRole("ADMIN", "DEVELOPER", "VIEWER")
                    
                    // Admin only
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/v1/config/**").hasRole("ADMIN")
                    
                    // Default - require authentication
                    .anyRequest().authenticated()
            }
            .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        // Parse comma-separated origins from environment variable
        val origins = corsAllowedOrigins.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        configuration.allowedOrigins = origins.ifEmpty { listOf("http://localhost:3000") }
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600
        
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
