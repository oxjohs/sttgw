package com.oxjohs.sttgw.stt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oxjohs.sttgw.domain.SttTranscript;
import com.oxjohs.sttgw.repository.SttTranscriptRepository;
import com.oxjohs.sttgw.session.CallSession;
import com.oxjohs.sttgw.websocket.SttWebSocketHandler;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SttStreamingServiceTest {

    @Test
    void 오디오청크를_받으면_스트리밍으로_전송하고_결과를_저장_브로드캐스트한다() {
        SttProperties properties = new SttProperties();
        properties.setStreamingEnabled(true);
        properties.setDefaultLanguage("ko-KR");

        FakeStreamingSession fakeStreamingSession = new FakeStreamingSession();
        FakeAdapter adapter = new FakeAdapter(fakeStreamingSession);
        SttAdapterFactory adapterFactory = new SttAdapterFactory(List.of(adapter), properties);
        SttTranscriptRepository transcriptRepository = Mockito.mock(SttTranscriptRepository.class);
        SttWebSocketHandler webSocketHandler = Mockito.mock(SttWebSocketHandler.class);

        SttStreamingService service = new SttStreamingService(
            adapterFactory, properties, transcriptRepository, webSocketHandler
        );

        CallSession callSession = new CallSession("call-1", "01012345678", "1001", LocalDateTime.now());
        callSession.appendPcm(11L, new short[] {1, 2, 3});

        short[] input = new short[] {10, 11, 12};
        service.onAudioChunk(callSession, 11L, input);

        assertThat(fakeStreamingSession.lastAudio).containsExactly(input);

        adapter.emit("고객 문의 내용", true, 0.92f);

        ArgumentCaptor<SttTranscript> transcriptCaptor = ArgumentCaptor.forClass(SttTranscript.class);
        verify(transcriptRepository).save(transcriptCaptor.capture());
        SttTranscript saved = transcriptCaptor.getValue();
        assertThat(saved.getCallId()).isEqualTo("call-1");
        assertThat(saved.getChannel()).isEqualTo("RX");
        assertThat(saved.getTranscript()).isEqualTo("고객 문의 내용");
        assertThat(saved.getVendorName()).isEqualTo("GOOGLE");
        assertThat(saved.getIsFinal()).isTrue();

        verify(webSocketHandler).broadcastSttResult(eq("call-1"), Mockito.any(SttResult.class));
    }

    @Test
    void 세션종료시_활성_스트리밍을_정리한다() {
        SttProperties properties = new SttProperties();
        properties.setStreamingEnabled(true);

        FakeStreamingSession fakeStreamingSession = new FakeStreamingSession();
        FakeAdapter adapter = new FakeAdapter(fakeStreamingSession);
        SttAdapterFactory adapterFactory = new SttAdapterFactory(List.of(adapter), properties);
        SttTranscriptRepository transcriptRepository = Mockito.mock(SttTranscriptRepository.class);
        SttWebSocketHandler webSocketHandler = Mockito.mock(SttWebSocketHandler.class);

        SttStreamingService service = new SttStreamingService(
            adapterFactory, properties, transcriptRepository, webSocketHandler
        );

        CallSession callSession = new CallSession("call-2", "01000000000", "2002", LocalDateTime.now());
        callSession.appendPcm(21L, new short[] {1});
        service.onAudioChunk(callSession, 21L, new short[] {5, 6});
        service.onSessionCompleted(callSession);

        assertThat(fakeStreamingSession.stopped).isTrue();
    }

    private static class FakeAdapter implements SttAdapter {

        private final FakeStreamingSession session;
        private SttResultCallback callback;

        private FakeAdapter(FakeStreamingSession session) {
            this.session = session;
        }

        @Override
        public SttResult recognize(Path audioFilePath, String languageCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SttStreamingSession startStreaming(String languageCode, SttResultCallback callback) {
            this.callback = callback;
            return session;
        }

        @Override
        public String getVendorName() {
            return "GOOGLE";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        private void emit(String transcript, boolean finalResult, float confidence) {
            callback.onResult(SttResult.builder()
                .vendorName("GOOGLE")
                .transcript(transcript)
                .confidence(confidence)
                .finalResult(finalResult)
                .timestamp(LocalDateTime.now())
                .mode(SttMode.STREAMING)
                .channel("RX")
                .build());
        }
    }

    private static class FakeStreamingSession implements SttStreamingSession {

        private short[] lastAudio;
        private boolean active = true;
        private boolean stopped;

        @Override
        public void sendAudio(short[] pcmSamples) {
            this.lastAudio = pcmSamples;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void stop() {
            this.active = false;
            this.stopped = true;
        }
    }
}
