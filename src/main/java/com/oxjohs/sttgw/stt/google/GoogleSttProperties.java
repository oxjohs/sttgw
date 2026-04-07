package com.oxjohs.sttgw.stt.google;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "sttgw.stt.google")
public class GoogleSttProperties {

    private String credentialPath;
    private int streamingBufferMs = 200;
}
