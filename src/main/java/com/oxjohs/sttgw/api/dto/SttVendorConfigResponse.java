package com.oxjohs.sttgw.api.dto;

import com.oxjohs.sttgw.domain.SttVendorConfig;
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
public class SttVendorConfigResponse {

    private Long id;
    private String vendorName;
    private String languageCode;
    private Boolean streamingEnabled;
    private String credentialPath;
    private String extraConfig;
    private Boolean active;
    private LocalDateTime updatedAt;

    public static SttVendorConfigResponse from(SttVendorConfig config) {
        return SttVendorConfigResponse.builder()
            .id(config.getId())
            .vendorName(config.getVendorName())
            .languageCode(config.getLanguageCode())
            .streamingEnabled(config.getStreamingEnabled())
            .credentialPath(config.getCredentialPath())
            .extraConfig(config.getExtraConfig())
            .active(config.getActive())
            .updatedAt(config.getUpdatedAt())
            .build();
    }
}
