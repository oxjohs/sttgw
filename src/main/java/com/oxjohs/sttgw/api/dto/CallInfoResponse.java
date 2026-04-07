package com.oxjohs.sttgw.api.dto;

import com.oxjohs.sttgw.session.CallSession;
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
public class CallInfoResponse {

    private String callId;
    private String callerNumber;
    private String agentExtension;
    private LocalDateTime startTime;
    private CallState state;

    public static CallInfoResponse from(CallSession session) {
        return CallInfoResponse.builder()
            .callId(session.getCallId())
            .callerNumber(session.getCallerNumber())
            .agentExtension(session.getAgentExtension())
            .startTime(session.getStartTime())
            .state(session.getState())
            .build();
    }
}
