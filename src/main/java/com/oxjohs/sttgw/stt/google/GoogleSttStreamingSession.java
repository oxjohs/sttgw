package com.oxjohs.sttgw.stt.google;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;
import com.oxjohs.sttgw.common.SttgwException;
import com.oxjohs.sttgw.stt.SttMode;
import com.oxjohs.sttgw.stt.SttResult;
import com.oxjohs.sttgw.stt.SttResultCallback;
import com.oxjohs.sttgw.stt.SttStreamingSession;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class GoogleSttStreamingSession implements SttStreamingSession {

    private final SpeechClient speechClient;
    private final ClientStream<StreamingRecognizeRequest> clientStream;
    private volatile boolean active = true;

    public GoogleSttStreamingSession(GoogleSttProperties properties, String languageCode, SttResultCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("STT 결과 콜백은 필수입니다.");
        }
        try {
            this.speechClient = SpeechClient.create(buildSpeechSettings(properties));
            this.clientStream = speechClient.streamingRecognizeCallable().splitCall(new ResultObserver(callback));
            this.clientStream.send(buildInitialRequest(languageCode));
        } catch (IOException exception) {
            throw new SttgwException("Google STT 스트리밍 세션 시작 실패", exception);
        } catch (RuntimeException exception) {
            stop();
            throw exception;
        }
    }

    @Override
    public void sendAudio(short[] pcmSamples) {
        if (!active || pcmSamples == null || pcmSamples.length == 0) {
            return;
        }

        byte[] bytes = new byte[pcmSamples.length * 2];
        for (int i = 0; i < pcmSamples.length; i++) {
            bytes[i * 2] = (byte) (pcmSamples[i] & 0xFF);
            bytes[i * 2 + 1] = (byte) ((pcmSamples[i] >> 8) & 0xFF);
        }

        StreamingRecognizeRequest request = StreamingRecognizeRequest.newBuilder()
            .setAudioContent(ByteString.copyFrom(bytes))
            .build();
        clientStream.send(request);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void stop() {
        if (!active) {
            return;
        }
        active = false;
        try {
            if (clientStream != null) {
                clientStream.closeSend();
            }
        } catch (RuntimeException exception) {
            log.debug("Google STT 스트림 종료 중 예외 무시: {}", exception.getMessage());
        }
        if (speechClient != null) {
            speechClient.close();
        }
    }

    private SpeechSettings buildSpeechSettings(GoogleSttProperties properties) throws IOException {
        if (!StringUtils.hasText(properties.getCredentialPath())) {
            return SpeechSettings.newBuilder().build();
        }

        try (InputStream inputStream = java.nio.file.Files.newInputStream(java.nio.file.Path.of(properties.getCredentialPath()))) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream);
            return SpeechSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        }
    }

    private StreamingRecognizeRequest buildInitialRequest(String languageCode) {
        StreamingRecognitionConfig streamingConfig = StreamingRecognitionConfig.newBuilder()
            .setConfig(RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(8000)
                .setLanguageCode(languageCode)
                .setEnableAutomaticPunctuation(true)
                .build())
            .setInterimResults(true)
            .build();

        return StreamingRecognizeRequest.newBuilder()
            .setStreamingConfig(streamingConfig)
            .build();
    }

    private class ResultObserver implements ResponseObserver<StreamingRecognizeResponse> {

        private final SttResultCallback callback;

        private ResultObserver(SttResultCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onStart(StreamController controller) {
            log.debug("Google STT 스트리밍 시작");
        }

        @Override
        public void onResponse(StreamingRecognizeResponse response) {
            List<StreamingRecognitionResult> results = response.getResultsList();
            for (StreamingRecognitionResult result : results) {
                if (result.getAlternativesCount() == 0) {
                    continue;
                }
                callback.onResult(SttResult.builder()
                    .vendorName("GOOGLE")
                    .transcript(result.getAlternatives(0).getTranscript())
                    .confidence(result.getAlternatives(0).getConfidence())
                    .finalResult(result.getIsFinal())
                    .timestamp(LocalDateTime.now())
                    .mode(SttMode.STREAMING)
                    .channel(null)
                    .build());
            }
        }

        @Override
        public void onError(Throwable throwable) {
            log.error("Google STT 스트리밍 오류: {}", throwable.getMessage(), throwable);
            active = false;
        }

        @Override
        public void onComplete() {
            log.debug("Google STT 스트리밍 완료");
            active = false;
        }
    }
}
