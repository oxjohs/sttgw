package com.oxjohs.sttgw.audio;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WavFileWriter {

    private static final int SAMPLE_RATE = 8000;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 1;

    public Path writeWav(byte[] pcmData, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());

        try (FileOutputStream fileOutputStream = new FileOutputStream(outputPath.toFile());
             DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream)) {

            int dataSize = pcmData.length;
            int fileSize = 36 + dataSize;

            dataOutputStream.writeBytes("RIFF");
            dataOutputStream.writeInt(Integer.reverseBytes(fileSize));
            dataOutputStream.writeBytes("WAVE");
            dataOutputStream.writeBytes("fmt ");
            dataOutputStream.writeInt(Integer.reverseBytes(16));
            dataOutputStream.writeShort(Short.reverseBytes((short) 1));
            dataOutputStream.writeShort(Short.reverseBytes((short) CHANNELS));
            dataOutputStream.writeInt(Integer.reverseBytes(SAMPLE_RATE));
            dataOutputStream.writeInt(Integer.reverseBytes(SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8));
            dataOutputStream.writeShort(Short.reverseBytes((short) (CHANNELS * BITS_PER_SAMPLE / 8)));
            dataOutputStream.writeShort(Short.reverseBytes((short) BITS_PER_SAMPLE));
            dataOutputStream.writeBytes("data");
            dataOutputStream.writeInt(Integer.reverseBytes(dataSize));
            dataOutputStream.write(pcmData);
        }

        log.info("WAV 파일 생성: path={}, size={}KB", outputPath, pcmData.length / 1024);
        return outputPath;
    }
}
