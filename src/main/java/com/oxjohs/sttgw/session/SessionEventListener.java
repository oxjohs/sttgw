package com.oxjohs.sttgw.session;

public interface SessionEventListener {

    default void onSessionCreated(CallSession session) {
    }

    default void onSessionCompleted(CallSession session) {
    }

    default void onAudioChunk(CallSession session, long ssrc, short[] samples) {
    }
}
