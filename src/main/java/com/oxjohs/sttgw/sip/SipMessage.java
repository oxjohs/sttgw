package com.oxjohs.sttgw.sip;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Builder
public class SipMessage {

    private SipMethodType method;
    private String callId;
    private String fromUri;
    private String toUri;
    private Integer mediaPort;

    @Builder.Default
    private Map<String, String> headers = new LinkedHashMap<>();
}
