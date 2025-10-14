package com.example.mentimeter.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration class for setting up WebSocket with STOMP protocol.
 */
@Configuration
@EnableWebSocketMessageBroker // 1. Why?
public class webSocketConfig implements WebSocketMessageBrokerConfigurer { // 2. Why?

    /**
     * Configures the message broker. The broker is responsible for routing messages
     * from one client to another.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 3. Why? - Destination for messages FROM the server TO the client
        registry.enableSimpleBroker("/topic");

        // 4. Why? - Prefix for messages FROM the client TO the server
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registers the STOMP endpoints. This is the URL that clients will use to
     * connect to the WebSocket server.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 5. How? - The actual connection endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // 6. Why?
                .withSockJS(); // 7. Why?
    }
}