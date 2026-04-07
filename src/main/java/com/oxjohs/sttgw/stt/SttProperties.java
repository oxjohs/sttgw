package com.oxjohs.sttgw.stt;

import com.oxjohs.sttgw.common.Constants;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "sttgw.stt")
public class SttProperties {

    private String defaultVendor = Constants.DEFAULT_VENDOR_NAME;
    private String defaultLanguage = Constants.DEFAULT_LANGUAGE_CODE;
    private boolean streamingEnabled = true;
}
