package com.oxjohs.sttgw.api.dto;

import jakarta.validation.constraints.NotBlank;
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
public class SttSettingRequest {

    @NotBlank(message = "vendorName은 필수입니다")
    private String vendorName;

    @NotBlank(message = "languageCode는 필수입니다")
    private String languageCode;

    private boolean streamingEnabled;
    private String credentialPath;
    private String extraConfig;

    @Builder.Default
    private boolean active = true;
}
