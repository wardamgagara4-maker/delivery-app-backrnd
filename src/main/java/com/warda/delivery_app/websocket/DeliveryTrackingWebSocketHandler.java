package com.warda.delivery_app.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One WebSocket connection per browser tab that is viewing the live
 * tracking map for a specific delivery (path: /ws/delivery/{deliveryId}).
 * When the driver's app pushes a new GPS position, or the delivery
 * status changes, we broadcast the updated delivery to every session
 * currently watching that same delivery id.
 */
@Component
public class DeliveryTrackingWebSocketHandler extends TextWebSocketHandler {

    private static final Pattern DELIVERY_ID_PATTERN = Pattern.compile("/ws/delivery/(\\d+)");

    private final Map<Long, List<WebSocketSession>> sessionsByDeliveryId = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        Long deliveryId = extractDeliveryId(session);

        if (deliveryId == null) {
            closeQuietly(session);
            return;
        }

        sessionsByDeliveryId
                .computeIfAbsent(deliveryId, id -> new CopyOnWriteArrayList<>())
                .add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        Long deliveryId = extractDeliveryId(session);

        if (deliveryId != null && sessionsByDeliveryId.containsKey(deliveryId)) {
            sessionsByDeliveryId.get(deliveryId).remove(session);
        }
    }

    /**
     * Push the latest state of a delivery to everyone currently watching it.
     */
    public void broadcast(Long deliveryId, Object payload) {

        List<WebSocketSession> sessions = sessionsByDeliveryId.get(deliveryId);

        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String json;

        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return;
        }

        TextMessage message = new TextMessage(json);

        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (IOException ignored) {
                // Session likely closing; it will be cleaned up on afterConnectionClosed.
            }
        }
    }

    private Long extractDeliveryId(WebSocketSession session) {

        String path = session.getUri() != null ? session.getUri().getPath() : null;

        if (path == null) {
            return null;
        }

        Matcher matcher = DELIVERY_ID_PATTERN.matcher(path);

        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }

        return null;
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.BAD_DATA);
        } catch (IOException ignored) {
        }
    }
}
