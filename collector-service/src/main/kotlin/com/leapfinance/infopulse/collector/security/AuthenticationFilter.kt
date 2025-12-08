package com.leapfinance.infopulse.collector.security

import com.leapfinance.infopulse.collector.config.SecurityProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private val logger = KotlinLogging.logger {}

/**
 * Filter that handles both JWT authentication (for dashboard users)
 * and Service API Key authentication (for tracking clients).
 */
@Component
class AuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val securityProperties: SecurityProperties
) : OncePerRequestFilter() {

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val SERVICE_API_KEY_HEADER = "X-Service-Api-Key"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // First try service API key (for tracking clients)
            val serviceApiKey = request.getHeader(SERVICE_API_KEY_HEADER)
            if (!serviceApiKey.isNullOrBlank()) {
                if (serviceApiKey == securityProperties.serviceApiKey) {
                    val serviceName = request.getHeader("X-Service-Name") ?: "unknown-service"
                    val auth = UsernamePasswordAuthenticationToken(
                        serviceName,
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_SERVICE"))
                    )
                    SecurityContextHolder.getContext().authentication = auth
                    logger.debug { "Authenticated service: $serviceName" }
                } else {
                    logger.warn { "Invalid service API key" }
                }
                filterChain.doFilter(request, response)
                return
            }
            
            // Try JWT authentication (for dashboard users)
            val authHeader = request.getHeader(AUTHORIZATION_HEADER)
            if (!authHeader.isNullOrBlank() && authHeader.startsWith(BEARER_PREFIX)) {
                val token = authHeader.substring(BEARER_PREFIX.length)
                
                if (jwtTokenProvider.validateToken(token)) {
                    val username = jwtTokenProvider.getUsernameFromToken(token)
                    val role = jwtTokenProvider.getRoleFromToken(token)
                    
                    val auth = UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_$role"))
                    )
                    SecurityContextHolder.getContext().authentication = auth
                    logger.debug { "Authenticated user: $username with role: $role" }
                }
            }
            
        } catch (e: Exception) {
            logger.error("Could not set user authentication in security context", e)
        }
        
        filterChain.doFilter(request, response)
    }
}
