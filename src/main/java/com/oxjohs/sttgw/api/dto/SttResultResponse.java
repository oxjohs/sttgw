package com.oxjohs.sttgw.api.dto;

import com.oxjohs.sttgw.stt.SttMode;
import com.oxjohs.sttgw.stt.SttResult;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SttResultResponse {

    private String vendorName;
    private String transcript;
    private Float confidence;
    private boolean finalResult;
    private LocalDateTime timestamp;
    private SttMode mode;
    private String channel;

    public static SttResultResponse from(SttResult result) {
        return SttResultResponse.builder()
            .vendorName(result.getVendorName())
            .transcript(result.getTranscript())
            .confidence(result.getConfidence())
            .finalResult(result.isFinal())
            .timestamp(result.getTimestamp())
            .mode(result.getMode())
            .channel(result.getChannel())
            .build();
    }
}
