package com.oxjohs.sttgw.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PcmBuffer {

    private final List<short[]> chunks = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger totalSamples = new AtomicInteger(0);

    public void append(short[] samples) {
        chunks.add(samples);
        totalSamples.addAndGet(samples.length);
    }

    public byte[] toByteArray() {
        List<short[]> snapshot;
        synchronized (chunks) {
            snapshot = new ArrayList<>(chunks);
        }

        byte[] result = new byte[totalSamples.get() * 2];
        int offset = 0;

        for (short[] chunk : snapshot) {
            for (short sample : chunk) {
                result[offset++] = (byte) (sample & 0xFF);
                result[offset++] = (byte) ((sample >> 8) & 0xFF);
            }
        }

        return result;
    }

    public int getSampleCount() {
        return totalSamples.get();
    }
}
