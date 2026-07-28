package com.warda.delivery_app.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final DeliveryTrackingWebSocketHandler deliveryTrackingWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry
                .addHandler(deliveryTrackingWebSocketHandler, "/ws/delivery/{deliveryId}")
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:5173"
                );
    }
}
