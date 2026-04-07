package com.oxjohs.sttgw.audio;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WavFileWriterTest {

    private final WavFileWriter wavFileWriter = new WavFileWriter();

    @TempDir
    Path tempDir;

    @Test
    void PCM_데이터로_WAV_파일을_생성하고_헤더를_정상_기록한다() throws Exception {
        byte[] pcmData = new byte[] {0x01, 0x00, 0x02, 0x00};
        Path output = tempDir.resolve("test.wav");

        Path written = wavFileWriter.writeWav(pcmData, output);
        byte[] bytes = Files.readAllBytes(written);

        assertThat(written).exists();
        assertThat(bytes.length).isEqualTo(44 + pcmData.length);
        assertThat(new String(bytes, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("RIFF");
        assertThat(new String(bytes, 8, 4, StandardCharsets.US_ASCII)).isEqualTo("WAVE");
        assertThat(new String(bytes, 12, 4, StandardCharsets.US_ASCII)).isEqualTo("fmt ");
        assertThat(new String(bytes, 36, 4, StandardCharsets.US_ASCII)).isEqualTo("data");
    }
}
