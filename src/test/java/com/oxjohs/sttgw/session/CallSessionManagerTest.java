package com.oxjohs.sttgw.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.oxjohs.sttgw.domain.CallRecord;
import com.oxjohs.sttgw.repository.CallRecordRepository;
import com.oxjohs.sttgw.sip.SipMessage;
import com.oxjohs.sttgw.sip.SipMethodType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CallSessionManagerTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-04-07T11:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final SessionEventListener listener = mock(SessionEventListener.class);
    private final CallRecordRepository callRecordRepository = mock(CallRecordRepository.class);

    @Test
    void SIP_INVITE로_세션을_생성하고_리스너를_호출한다() {
        CallSessionManager manager = new CallSessionManager(fixedClock, List.of(listener), callRecordRepository);

        manager.onCallStart(inviteMessage());

        assertThat(manager.getActiveSessionCount()).isEqualTo(1);
        CallSession session = manager.getActiveSessions().iterator().next();
        assertThat(session.getCallId()).isEqualTo("call-1234");
        assertThat(session.getState()).isEqualTo(CallState.CONNECTED);
        verify(listener).onSessionCreated(any(CallSession.class));
    }

    @Test
    void appendAudio는_매핑된_세션에_PCM을_누적하고_리스너를_호출한다() {
        CallSessionManager manager = new CallSessionManager(fixedClock, List.of(listener), callRecordRepository);
        manager.onCallStart(inviteMessage());

        manager.appendAudio(1001L, new short[] {1, 2, 3}, "211.192.89.43", 10000);

        CallSession session = manager.getActiveSessions().iterator().next();
        assertThat(session.getRxBuffer()).isNotNull();
        assertThat(session.getRxBuffer().getSampleCount()).isEqualTo(3);
        verify(listener).onSessionCreated(any(CallSession.class));
        verify(listener).onAudioChunk(any(CallSession.class), org.mockito.ArgumentMatchers.eq(1001L), any(short[].class));
    }

    @Test
    void SIP_BYE로_세션을_종료하고_통화이력을_저장한다() {
        CallSessionManager manager = new CallSessionManager(fixedClock, List.of(listener), callRecordRepository);
        when(callRecordRepository.save(any(CallRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        manager.onCallStart(inviteMessage());
        manager.onCallEnd(byeMessage());

        assertThat(manager.getActiveSessionCount()).isZero();

        ArgumentCaptor<CallRecord> captor = ArgumentCaptor.forClass(CallRecord.class);
        verify(callRecordRepository, atLeast(2)).save(captor.capture());
        CallRecord saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(saved.getCallId()).isEqualTo("call-1234");
        assertThat(saved.getState()).isEqualTo(CallState.COMPLETED);
        assertThat(saved.getDurationSec()).isEqualTo(0);
        verify(listener).onSessionCreated(any(CallSession.class));
        verify(listener).onSessionCompleted(any(CallSession.class));
    }

    @Test
    void SIP_CANCEL로_세션을_취소한다() {
        CallSessionManager manager = new CallSessionManager(fixedClock, List.of(listener), callRecordRepository);
        when(callRecordRepository.save(any(CallRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        manager.onCallStart(inviteMessage());
        manager.onCallCancel(cancelMessage());

        ArgumentCaptor<CallRecord> captor = ArgumentCaptor.forClass(CallRecord.class);
        verify(callRecordRepository, atLeast(2)).save(captor.capture());
        CallRecord saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(saved.getState()).isEqualTo(CallState.CANCELLED);
    }

    @Test
    void 매핑되지_않은_RTP_포트는_무시한다() {
        CallSessionManager manager = new CallSessionManager(fixedClock, List.of(listener), callRecordRepository);

        manager.appendAudio(1001L, new short[] {1, 2}, "211.192.89.43", 9999);

        assertThat(manager.getActiveSessionCount()).isZero();
        verifyNoMoreInteractions(callRecordRepository);
    }

    private SipMessage inviteMessage() {
        return SipMessage.builder()
            .method(SipMethodType.INVITE)
            .callId("call-1234")
            .fromUri("07041405642")
            .toUri("1001")
            .mediaPort(10000)
            .build();
    }

    private SipMessage byeMessage() {
        return SipMessage.builder()
            .method(SipMethodType.BYE)
            .callId("call-1234")
            .build();
    }

    private SipMessage cancelMessage() {
        return SipMessage.builder()
            .method(SipMethodType.CANCEL)
            .callId("call-1234")
            .build();
    }
}
