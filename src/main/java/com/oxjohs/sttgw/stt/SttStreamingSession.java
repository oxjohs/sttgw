package com.oxjohs.sttgw.stt;

public interface SttStreamingSession extends AutoCloseable {

    void sendAudio(short[] pcmSamples);

    boolean isActive();

    void stop();

    @Override
    default void close() {
        stop();
    }
}
