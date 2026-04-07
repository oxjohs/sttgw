package com.oxjohs.sttgw.api;

import com.oxjohs.sttgw.api.dto.ApiResponse;
import com.oxjohs.sttgw.api.dto.SttSettingRequest;
import com.oxjohs.sttgw.api.dto.SttVendorConfigResponse;
import com.oxjohs.sttgw.domain.SttVendorConfig;
import com.oxjohs.sttgw.repository.SttVendorConfigRepository;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SttVendorConfigRepository sttVendorConfigRepository;
    private final Clock systemClock;

    @GetMapping("/stt")
    public ApiResponse<SttVendorConfigResponse> getSttConfig() {
        SttVendorConfig config = sttVendorConfigRepository.findFirstByOrderByIdAsc()
            .orElse(SttVendorConfig.defaultConfig());
        return ApiResponse.success(SttVendorConfigResponse.from(config));
    }

    @PutMapping("/stt")
    public ApiResponse<SttVendorConfigResponse> updateSttConfig(@Valid @RequestBody SttSettingRequest request) {
        SttVendorConfig config = sttVendorConfigRepository.findFirstByOrderByIdAsc()
            .orElseGet(SttVendorConfig::defaultConfig);

        config.setVendorName(request.getVendorName());
        config.setLanguageCode(request.getLanguageCode());
        config.setStreamingEnabled(request.isStreamingEnabled());
        config.setCredentialPath(request.getCredentialPath());
        config.setExtraConfig(request.getExtraConfig());
        config.setActive(request.isActive());
        config.setUpdatedAt(LocalDateTime.now(systemClock));

        SttVendorConfig saved = sttVendorConfigRepository.save(config);
        return ApiResponse.success(SttVendorConfigResponse.from(saved));
    }
}
