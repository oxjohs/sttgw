package com.oxjohs.sttgw.audio;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "sttgw.audio")
public class AudioProperties {

    private String basePath = "/APP_DATA/audio";
    private int retentionDays = 30;
}
