package com.oxjohs.sttgw.stt;

import java.nio.file.Path;

public interface SttAdapter {

    SttResult recognize(Path audioFilePath, String languageCode);

    SttStreamingSession startStreaming(String languageCode, SttResultCallback callback);

    String getVendorName();

    boolean isAvailable();
}
