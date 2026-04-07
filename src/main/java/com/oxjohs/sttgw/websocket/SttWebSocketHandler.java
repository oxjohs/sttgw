package com.oxjohs.sttgw.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oxjohs.sttgw.session.CallSession;
import com.oxjohs.sttgw.session.SessionEventListener;
import com.oxjohs.sttgw.stt.SttResult;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class SttWebSocketHandler extends TextWebSocketHandler implements SessionEventListener {

    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        webSocketSessionRegistry.register(session);
        log.info("WebSocket 연결: sessionId={}", session.getId());
        sendSingle(session, Map.of(
            "type", "WS_READY",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        webSocketSessionRegistry.unregister(session);
    }

    @Override
    public void onSessionCreated(CallSession session) {
        broadcastSessionEvent("SESSION_CREATED", session);
    }

    @Override
    public void onSessionCompleted(CallSession session) {
        broadcastSessionEvent("SESSION_COMPLETED", session);
    }

    @Override
    public void onAudioChunk(CallSession session, long ssrc, short[] samples) {
    }

    public void broadcastSttResult(String callId, SttResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "STT_RESULT");
        payload.put("callId", callId);
        payload.put("text", result.getTranscript());
        payload.put("isFinal", result.isFinal());
        payload.put("confidence", result.getConfidence());
        payload.put("channel", result.getChannel());
        payload.put("timestamp", result.getTimestamp());
        broadcast(payload);
    }

    private void broadcastSessionEvent(String type, CallSession session) {
        Map<String, Object> call = new LinkedHashMap<>();
        call.put("callId", session.getCallId());
        call.put("callerNumberMasked", maskCallerNumber(session.getCallerNumber()));
        call.put("agentExtension", session.getAgentExtension());
        call.put("state", session.getState());
        call.put("startTime", session.getStartTime());
        call.put("endTime", session.getEndTime());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("call", call);
        payload.put("timestamp", LocalDateTime.now());
        broadcast(payload);
    }

    private void broadcast(Map<String, Object> payload) {
        webSocketSessionRegistry.getAll().forEach(session -> sendSingle(session, payload));
    }

    private void sendSingle(WebSocketSession session, Map<String, Object> payload) {
        try {
            if (!session.isOpen()) {
                return;
            }
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (IOException exception) {
            log.warn("WebSocket 전송 실패: sessionId={}, message={}", session.getId(), exception.getMessage());
        }
    }

    private String maskCallerNumber(String callerNumber) {
        if (callerNumber == null || callerNumber.isBlank()) {
            return "";
        }
        if (callerNumber.length() <= 4) {
            return callerNumber;
        }

        int prefix = Math.min(3, callerNumber.length() - 2);
        int suffix = 2;
        int maskLength = Math.max(1, callerNumber.length() - prefix - suffix);
        return callerNumber.substring(0, prefix) + "*".repeat(maskLength)
            + callerNumber.substring(callerNumber.length() - suffix);
    }
}
