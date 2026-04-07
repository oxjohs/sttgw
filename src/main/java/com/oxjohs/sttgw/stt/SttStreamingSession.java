package com.oxjohs.sttgw.stt;

public interface SttStreamingSession extends AutoCloseable {

    void stop();

    @Override
    default void close() {
        stop();
    }
}
