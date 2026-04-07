package com.oxjohs.sttgw.api.dto;

import com.oxjohs.sttgw.domain.CallRecord;
import com.oxjohs.sttgw.session.CallState;
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
public class CallRecordResponse {

    private Long id;
    private String callId;
    private String callerNumber;
    private String agentExtension;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationSec;
    private CallState state;
    private String rxAudioPath;
    private String txAudioPath;
    private LocalDateTime createdAt;

    public static CallRecordResponse from(CallRecord callRecord) {
        return CallRecordResponse.builder()
            .id(callRecord.getId())
            .callId(callRecord.getCallId())
            .callerNumber(callRecord.getCallerNumber())
            .agentExtension(callRecord.getAgentExtension())
            .startTime(callRecord.getStartTime())
            .endTime(callRecord.getEndTime())
            .durationSec(callRecord.getDurationSec())
            .state(callRecord.getState())
            .rxAudioPath(callRecord.getRxAudioPath())
            .txAudioPath(callRecord.getTxAudioPath())
            .createdAt(callRecord.getCreatedAt())
            .build();
    }
}
