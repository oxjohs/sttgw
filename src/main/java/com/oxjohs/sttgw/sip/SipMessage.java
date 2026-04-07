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
    private String fromTag;
    private String toTag;
    private Integer mediaPort;
    private String mediaIp;
    private String codec;

    @Builder.Default
    private Map<String, String> headers = new LinkedHashMap<>();
}
