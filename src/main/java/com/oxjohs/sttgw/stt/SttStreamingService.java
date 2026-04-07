package com.oxjohs.sttgw.stt;

import com.oxjohs.sttgw.domain.SttTranscript;
import com.oxjohs.sttgw.repository.SttTranscriptRepository;
import com.oxjohs.sttgw.session.CallSession;
import com.oxjohs.sttgw.session.SessionEventListener;
import com.oxjohs.sttgw.websocket.SttWebSocketHandler;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SttStreamingService implements SessionEventListener {

    private static final String CHANNEL_RX = "RX";
    private static final String CHANNEL_TX = "TX";

    private final SttAdapterFactory sttAdapterFactory;
    private final SttProperties sttProperties;
    private final SttTranscriptRepository sttTranscriptRepository;
    private final SttWebSocketHandler sttWebSocketHandler;

    private final Map<String, SttStreamingSession> activeStreams = new ConcurrentHashMap<>();
    private final Map<String, String> streamVendors = new ConcurrentHashMap<>();

    @Override
    public void onSessionCreated(CallSession session) {
    }

    @Override
    public void onAudioChunk(CallSession session, long ssrc, short[] samples) {
        if (!sttProperties.isStreamingEnabled()) {
            return;
        }
        if (session == null || samples == null || samples.length == 0) {
            return;
        }

        String channel = resolveChannel(session, ssrc);
        if (channel == null) {
            return;
        }

        String streamKey = buildKey(session.getCallId(), channel);
        SttStreamingSession streamingSession = getOrCreateStream(streamKey, session.getCallId(), channel);
        if (streamingSession == null || !streamingSession.isActive()) {
            return;
        }

        try {
            streamingSession.sendAudio(samples);
        } catch (Exception exception) {
            log.warn("STT 스트리밍 오디오 전송 실패: callId={}, channel={}, message={}",
                session.getCallId(), channel, exception.getMessage());
            stopAndRemove(streamKey);
        }
    }

    @Override
    public void onSessionCompleted(CallSession session) {
        if (session == null || session.getCallId() == null) {
            return;
        }

        String prefix = session.getCallId() + ":";
        activeStreams.keySet().stream()
            .filter(key -> key.startsWith(prefix))
            .toList()
            .forEach(this::stopAndRemove);
    }

    @PreDestroy
    public void shutdown() {
        activeStreams.keySet().forEach(this::stopAndRemove);
    }

    private SttStreamingSession getOrCreateStream(String streamKey, String callId, String channel) {
        return activeStreams.compute(streamKey, (key, current) -> {
            if (current != null && current.isActive()) {
                return current;
            }
            if (current != null) {
                safeStop(current);
            }
            return createStream(callId, channel, key);
        });
    }

    private SttStreamingSession createStream(String callId, String channel, String streamKey) {
        try {
            SttAdapter adapter = sttAdapterFactory.getDefaultAdapter();
            String vendor = adapter.getVendorName();
            streamVendors.put(streamKey, vendor);
            return adapter.startStreaming(sttProperties.getDefaultLanguage(),
                result -> onSttResult(callId, channel, vendor, result));
        } catch (Exception exception) {
            log.warn("STT 스트리밍 세션 생성 실패: callId={}, channel={}, message={}",
                callId, channel, exception.getMessage());
            return null;
        }
    }

    private void onSttResult(String callId, String channel, String vendor, SttResult result) {
        if (result == null || result.getTranscript() == null || result.getTranscript().isBlank()) {
            return;
        }

        SttTranscript transcript = new SttTranscript();
        transcript.setCallId(callId);
        transcript.setChannel(channel);
        transcript.setTranscript(result.getTranscript());
        transcript.setConfidence(result.getConfidence() == null ? 0.0f : result.getConfidence());
        transcript.setIsFinal(result.isFinal());
        transcript.setTimestamp(result.getTimestamp() == null ? LocalDateTime.now() : result.getTimestamp());
        transcript.setVendorName(result.getVendorName() == null ? vendor : result.getVendorName());
        sttTranscriptRepository.save(transcript);

        SttResult outbound = SttResult.builder()
            .vendorName(transcript.getVendorName())
            .transcript(transcript.getTranscript())
            .confidence(transcript.getConfidence())
            .finalResult(Boolean.TRUE.equals(transcript.getIsFinal()))
            .timestamp(transcript.getTimestamp())
            .mode(SttMode.STREAMING)
            .channel(channel)
            .build();
        sttWebSocketHandler.broadcastSttResult(callId, outbound);
    }

    private String resolveChannel(CallSession session, long ssrc) {
        if (session.getRxSsrc() != null && session.getRxSsrc() == ssrc) {
            return CHANNEL_RX;
        }
        if (session.getTxSsrc() != null && session.getTxSsrc() == ssrc) {
            return CHANNEL_TX;
        }
        return null;
    }

    private String buildKey(String callId, String channel) {
        return callId + ":" + channel;
    }

    private void stopAndRemove(String streamKey) {
        SttStreamingSession session = activeStreams.remove(streamKey);
        streamVendors.remove(streamKey);
        safeStop(session);
    }

    private void safeStop(SttStreamingSession session) {
        if (session == null) {
            return;
        }
        try {
            session.stop();
        } catch (Exception exception) {
            log.debug("STT 스트리밍 세션 종료 중 예외 무시: {}", exception.getMessage());
        }
    }
}
