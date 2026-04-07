package com.oxjohs.sttgw.stt.google;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.protobuf.ByteString;
import com.oxjohs.sttgw.common.SttgwException;
import com.oxjohs.sttgw.stt.SttAdapter;
import com.oxjohs.sttgw.stt.SttMode;
import com.oxjohs.sttgw.stt.SttResult;
import com.oxjohs.sttgw.stt.SttResultCallback;
import com.oxjohs.sttgw.stt.SttStreamingSession;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleSttAdapter implements SttAdapter {

    private static final String VENDOR_NAME = "GOOGLE";

    private final GoogleSttProperties properties;

    @Override
    public SttResult recognize(Path audioFilePath, String languageCode) {
        if (audioFilePath == null || !Files.exists(audioFilePath)) {
            throw new SttgwException("오디오 파일을 찾을 수 없습니다: " + audioFilePath);
        }

        try (SpeechClient speechClient = SpeechClient.create(buildSpeechSettings())) {
            byte[] audioBytes = Files.readAllBytes(audioFilePath);

            RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(8000)
                .setLanguageCode(languageCode)
                .setEnableAutomaticPunctuation(true)
                .build();

            RecognitionAudio audio = RecognitionAudio.newBuilder()
                .setContent(ByteString.copyFrom(audioBytes))
                .build();

            RecognizeResponse response = speechClient.recognize(config, audio);
            return toBatchResult(response);
        } catch (IOException exception) {
            throw new SttgwException("Google STT 배치 인식 실패", exception);
        }
    }

    @Override
    public SttStreamingSession startStreaming(String languageCode, SttResultCallback callback) {
        return new GoogleSttStreamingSession(properties, languageCode, callback);
    }

    @Override
    public String getVendorName() {
        return VENDOR_NAME;
    }

    @Override
    public boolean isAvailable() {
        if (StringUtils.hasText(properties.getCredentialPath())) {
            return Files.exists(Path.of(properties.getCredentialPath()));
        }
        return hasDefaultAdcPath();
    }

    private SttResult toBatchResult(RecognizeResponse response) {
        StringBuilder transcript = new StringBuilder();
        float totalConfidence = 0.0f;
        int confidenceCount = 0;

        for (SpeechRecognitionResult result : response.getResultsList()) {
            if (result.getAlternativesCount() == 0) {
                continue;
            }
            SpeechRecognitionAlternative alternative = result.getAlternatives(0);
            transcript.append(alternative.getTranscript());
            if (alternative.getConfidence() > 0.0f) {
                totalConfidence += alternative.getConfidence();
                confidenceCount++;
            }
        }

        float confidence = confidenceCount == 0 ? 0.0f : totalConfidence / confidenceCount;
        return SttResult.builder()
            .vendorName(VENDOR_NAME)
            .transcript(transcript.toString().trim())
            .confidence(confidence)
            .finalResult(true)
            .timestamp(LocalDateTime.now())
            .mode(SttMode.BATCH)
            .channel(null)
            .build();
    }

    private SpeechSettings buildSpeechSettings() throws IOException {
        if (!StringUtils.hasText(properties.getCredentialPath())) {
            return SpeechSettings.newBuilder().build();
        }

        Path credentialFile = Path.of(properties.getCredentialPath());
        if (!Files.exists(credentialFile)) {
            throw new SttgwException("Google 자격증명 파일을 찾을 수 없습니다: " + credentialFile);
        }

        try (InputStream inputStream = Files.newInputStream(credentialFile)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream);
            return SpeechSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        }
    }

    private boolean hasDefaultAdcPath() {
        Path unixDefault = Path.of(System.getProperty("user.home"), ".config", "gcloud",
            "application_default_credentials.json");
        if (Files.exists(unixDefault)) {
            return true;
        }

        Path macDefault = Path.of(System.getProperty("user.home"), "Library", "Application Support",
            "gcloud", "application_default_credentials.json");
        return Files.exists(macDefault);
    }
}
