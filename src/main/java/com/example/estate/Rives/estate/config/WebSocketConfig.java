package com.example.estate.Rives.estate.config;

import com.example.estate.Rives.estate.security.PrincipalHandshakeHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final PrincipalHandshakeHandler principalHandshakeHandler;

    // Same property key as WebSecurityConfig's CORS origin - kept as two
    // separate @Value injections rather than one shared constant, since
    // @Value needs an instance-bound field either way.
    @Value("${app.frontend-origin}")
    private String frontendOrigin;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // No SockJS fallback: the frontend connects with a plain WebSocket
        // (@stomp/stompjs), keeping the dependency footprint minimal.
        registry.addEndpoint("/ws")
                .setHandshakeHandler(principalHandshakeHandler)
                .setAllowedOrigins(frontendOrigin);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
