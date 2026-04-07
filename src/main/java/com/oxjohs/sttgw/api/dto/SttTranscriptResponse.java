package com.oxjohs.sttgw.api.dto;

import com.oxjohs.sttgw.domain.SttTranscript;
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
public class SttTranscriptResponse {

    private Long id;
    private String callId;
    private String channel;
    private String transcript;
    private Float confidence;
    private Boolean finalResult;
    private LocalDateTime timestamp;
    private String vendorName;
    private LocalDateTime createdAt;

    public static SttTranscriptResponse from(SttTranscript transcript) {
        return SttTranscriptResponse.builder()
            .id(transcript.getId())
            .callId(transcript.getCallId())
            .channel(transcript.getChannel())
            .transcript(transcript.getTranscript())
            .confidence(transcript.getConfidence())
            .finalResult(transcript.getIsFinal())
            .timestamp(transcript.getTimestamp())
            .vendorName(transcript.getVendorName())
            .createdAt(transcript.getCreatedAt())
            .build();
    }
}
