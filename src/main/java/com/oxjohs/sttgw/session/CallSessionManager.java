package com.oxjohs.sttgw.session;

import com.oxjohs.sttgw.domain.CallRecord;
import com.oxjohs.sttgw.repository.CallRecordRepository;
import com.oxjohs.sttgw.sip.SipMessage;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallSessionManager {

    private final Clock systemClock;
    private final List<SessionEventListener> listeners;
    private final CallRecordRepository callRecordRepository;

    private final ConcurrentHashMap<String, CallSession> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> portToCallId = new ConcurrentHashMap<>();

    public void onCallStart(SipMessage message) {
        if (message == null || message.getCallId() == null || message.getCallId().isBlank()) {
            log.warn("Call-ID 없는 SIP INVITE 무시");
            return;
        }

        LocalDateTime now = LocalDateTime.now(systemClock);
        CallSession session = new CallSession(
            message.getCallId(),
            message.getFromUri(),
            message.getToUri(),
            now
        );
        session.setState(CallState.CONNECTED);

        activeSessions.put(message.getCallId(), session);

        if (message.getMediaPort() != null) {
            portToCallId.put(message.getMediaPort(), message.getCallId());
        }

        listeners.forEach(listener -> listener.onSessionCreated(session));
        log.info("통화 세션 생성: callId={}, caller={}", message.getCallId(), message.getFromUri());
    }

    public void onCallEnd(SipMessage message) {
        completeSession(message, CallState.COMPLETED);
    }

    public void onCallCancel(SipMessage message) {
        completeSession(message, CallState.CANCELLED);
    }

    public void appendAudio(long ssrc, short[] pcmSamples, String srcIp, int srcPort) {
        String callId = findCallIdByPort(srcPort);
        if (callId == null) {
            log.debug("매핑되지 않은 RTP 포트: srcIp={}, srcPort={}", srcIp, srcPort);
            return;
        }

        CallSession session = activeSessions.get(callId);
        if (session == null || session.getState() != CallState.CONNECTED) {
            return;
        }

        session.appendPcm(ssrc, pcmSamples);
        listeners.forEach(listener -> listener.onAudioChunk(session, ssrc, pcmSamples));
    }

    public Collection<CallSession> getActiveSessions() {
        return Collections.unmodifiableCollection(activeSessions.values());
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    String findCallIdByPort(int srcPort) {
        return portToCallId.get(srcPort);
    }

    private void completeSession(SipMessage message, CallState targetState) {
        if (message == null || message.getCallId() == null || message.getCallId().isBlank()) {
            return;
        }

        CallSession session = activeSessions.remove(message.getCallId());
        if (session == null) {
            log.debug("존재하지 않는 세션 종료 요청: callId={}", message.getCallId());
            return;
        }

        session.setState(targetState);
        session.setEndTime(LocalDateTime.now(systemClock));
        portToCallId.entrySet().removeIf(entry -> message.getCallId().equals(entry.getValue()));

        listeners.forEach(listener -> listener.onSessionCompleted(session));
        saveCallRecord(session);

        long durationSec = session.getEndTime() == null
            ? 0L
            : Duration.between(session.getStartTime(), session.getEndTime()).getSeconds();
        log.info("통화 세션 종료: callId={}, state={}, duration={}s",
            message.getCallId(), targetState, durationSec);
    }

    private void saveCallRecord(CallSession session) {
        CallRecord callRecord = new CallRecord();
        callRecord.setCallId(session.getCallId());
        callRecord.setCallerNumber(session.getCallerNumber());
        callRecord.setAgentExtension(session.getAgentExtension());
        callRecord.setStartTime(session.getStartTime());
        callRecord.setEndTime(session.getEndTime());
        callRecord.setDurationSec(calculateDurationSec(session));
        callRecord.setState(session.getState());
        callRecord.setRxAudioPath(session.getRxAudioPath());
        callRecord.setTxAudioPath(session.getTxAudioPath());
        callRecordRepository.save(callRecord);
    }

    private Integer calculateDurationSec(CallSession session) {
        if (session.getEndTime() == null) {
            return null;
        }
        return (int) Duration.between(session.getStartTime(), session.getEndTime()).getSeconds();
    }
}
