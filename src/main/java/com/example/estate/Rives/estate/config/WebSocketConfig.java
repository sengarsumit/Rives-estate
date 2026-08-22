package com.example.estate.Rives.estate.config;

import com.example.estate.Rives.estate.security.PrincipalHandshakeHandler;
import com.example.estate.Rives.estate.security.WebSecurityConfig;
import lombok.RequiredArgsConstructor;
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

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // No SockJS fallback: the frontend connects with a plain WebSocket
        // (@stomp/stompjs), keeping the dependency footprint minimal.
        registry.addEndpoint("/ws")
                .setHandshakeHandler(principalHandshakeHandler)
                .setAllowedOrigins(WebSecurityConfig.FRONTEND_ORIGIN);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
