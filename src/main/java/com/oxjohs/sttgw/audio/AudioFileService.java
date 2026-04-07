package com.oxjohs.sttgw.audio;

import com.oxjohs.sttgw.common.FileUtils;
import com.oxjohs.sttgw.session.CallSession;
import com.oxjohs.sttgw.session.SessionEventListener;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioFileService implements SessionEventListener {

    private final AtomicInteger threadCounter = new AtomicInteger(1);
    private final WavFileWriter wavFileWriter;
    private final AudioProperties audioProperties;
    private final ExecutorService audioExecutor = Executors.newFixedThreadPool(
        4,
        runnable -> new Thread(runnable, "audio-writer-" + threadCounter.getAndIncrement())
    );

    @Override
    public void onSessionCompleted(CallSession session) {
        audioExecutor.submit(() -> generateAudioFiles(session));
    }

    @Override
    public void onSessionCreated(CallSession session) {
    }

    @Override
    public void onAudioChunk(CallSession session, long ssrc, short[] samples) {
    }

    private void generateAudioFiles(CallSession session) {
        try {
            LocalDateTime startedAt = session.getStartTime();
            Path baseDir = FileUtils.resolveDatedDirectory(audioProperties.getBasePath(), startedAt);
            String baseName = FileUtils.sanitizeFileName(session.getCallId());

            PcmBuffer rxBuffer = session.getRxBuffer();
            if (rxBuffer != null && rxBuffer.getSampleCount() > 0) {
                Path rxPath = wavFileWriter.writeWav(rxBuffer.toByteArray(), baseDir.resolve(baseName + "_rx.wav"));
                session.setRxAudioPath(rxPath.toString());
            }

            PcmBuffer txBuffer = session.getTxBuffer();
            if (txBuffer != null && txBuffer.getSampleCount() > 0) {
                Path txPath = wavFileWriter.writeWav(txBuffer.toByteArray(), baseDir.resolve(baseName + "_tx.wav"));
                session.setTxAudioPath(txPath.toString());
            }

            log.info("오디오 파일 생성 완료: callId={}", session.getCallId());
        } catch (Exception exception) {
            log.error("오디오 파일 생성 실패: callId={}", session.getCallId(), exception);
        }
    }
}
