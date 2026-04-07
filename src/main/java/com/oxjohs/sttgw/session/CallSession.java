package com.oxjohs.sttgw.session;

import com.oxjohs.sttgw.audio.PcmBuffer;
import com.oxjohs.sttgw.common.Constants;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class CallSession {

    private final String callId;
    private final String callerNumber;
    private final String agentExtension;
    private final LocalDateTime startTime;

    @Setter
    private volatile CallState state;

    @Setter
    private volatile LocalDateTime endTime;

    @Setter
    private volatile String rxAudioPath;

    @Setter
    private volatile String txAudioPath;

    private final ConcurrentHashMap<Long, PcmBuffer> ssrcBuffers = new ConcurrentHashMap<>();
    private final AtomicInteger ssrcCount = new AtomicInteger(0);

    private volatile Long rxSsrc;
    private volatile Long txSsrc;

    public CallSession(String callId, String callerNumber, String agentExtension, LocalDateTime startTime) {
        this.callId = callId;
        this.callerNumber = callerNumber;
        this.agentExtension = agentExtension;
        this.startTime = startTime;
        this.state = CallState.RINGING;
    }

    public void appendPcm(long ssrc, short[] samples) {
        PcmBuffer buffer = ssrcBuffers.computeIfAbsent(ssrc, key -> {
            int order = ssrcCount.getAndIncrement();
            String channel = registerChannel(order, ssrc);
            log.info("SSRC 등록: callId={}, ssrc={}, channel={}", callId, ssrc, channel);
            return new PcmBuffer();
        });

        buffer.append(samples);
    }

    public PcmBuffer getRxBuffer() {
        return rxSsrc == null ? null : ssrcBuffers.get(rxSsrc);
    }

    public PcmBuffer getTxBuffer() {
        return txSsrc == null ? null : ssrcBuffers.get(txSsrc);
    }

    public Map<Long, PcmBuffer> getSsrcBuffersView() {
        return Map.copyOf(ssrcBuffers);
    }

    private String registerChannel(int order, long ssrc) {
        if (order == 0) {
            rxSsrc = ssrc;
            return Constants.CHANNEL_RX;
        }
        if (order == 1) {
            txSsrc = ssrc;
            return Constants.CHANNEL_TX;
        }
        return "EXTRA";
    }
}
