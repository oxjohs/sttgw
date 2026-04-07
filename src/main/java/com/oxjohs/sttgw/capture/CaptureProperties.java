package com.oxjohs.sttgw.capture;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sttgw.capture")
public class CaptureProperties {

    private boolean enabled = false;
    private String interfaceName = "eth3";
    private int snapLen = 65536;
    private int timeoutMillis = 10;
    private String dumpPath = "/APP_DATA/DUMP";
    private boolean dumpEnabled = true;
    private String bpfFilter = "udp";
}
