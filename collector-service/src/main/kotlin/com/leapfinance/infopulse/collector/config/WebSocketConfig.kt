package com.leapfinance.infopulse.collector.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * WebSocket configuration for real-time updates.
 * 
 * Endpoints:
 * - /ws: WebSocket connection endpoint
 * 
 * Topics (subscribe to receive updates):
 * - /topic/logs: Real-time API log events
 * - /topic/alerts: Real-time alert notifications
 * - /topic/incidents: Real-time incident updates
 * - /topic/health: Real-time health score updates
 * - /topic/metrics: Real-time metrics updates
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    
    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        // Enable a simple in-memory message broker
        // Clients subscribe to /topic/* to receive messages
        config.enableSimpleBroker("/topic")
        
        // Prefix for messages FROM clients TO server
        config.setApplicationDestinationPrefixes("/app")
    }
    
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // Register STOMP endpoint for WebSocket connections
        // Clients connect to ws://host:port/ws
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS()  // Fallback for browsers without WebSocket support
    }
}
