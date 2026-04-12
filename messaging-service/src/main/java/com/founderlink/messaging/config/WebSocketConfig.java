package com.founderlink.messaging.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket configuration.
 *
 * <p>Clients connect to {@code /ws} (with SockJS fallback),
 * subscribe to {@code /topic/conversation/{lo}/{hi}} to receive real-time messages,
 * and publish via {@code /app/...} prefix (currently unused — send still goes through REST).
 *
 * <p><strong>Single-node note:</strong> the in-memory broker is sufficient here.
 * For multi-instance deployments, replace {@code enableSimpleBroker} with
 * {@code enableStompBrokerRelay} pointing to the existing RabbitMQ instance
 * (RabbitMQ already carries the {@code spring-boot-starter-amqp} dep in pom.xml).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // In-memory broker for /topic/** subscriptions
        config.enableSimpleBroker("/topic");
        // Prefix for messages routed to @MessageMapping controllers (future use)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // Allow the Angular dev-server origin and any deployed origin
                .setAllowedOriginPatterns("*")
                // SockJS fallback for environments where native WebSocket is unavailable
                .withSockJS()
                // Gateway owns CORS — suppress duplicate header emission from SockJS
                .setSuppressCors(true);
    }
}
