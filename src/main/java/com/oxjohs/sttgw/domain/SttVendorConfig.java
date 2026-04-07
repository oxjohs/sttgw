package com.oxjohs.sttgw.domain;

import com.oxjohs.sttgw.common.Constants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Entity
@Table(name = "STT_VENDOR_CONFIG")
public class SttVendorConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_name", nullable = false)
    private String vendorName;

    @Column(name = "language_code")
    private String languageCode = Constants.DEFAULT_LANGUAGE_CODE;

    @Column(name = "streaming_enabled")
    private Boolean streamingEnabled = true;

    @Column(name = "credential_path")
    private String credentialPath;

    @Column(name = "extra_config", columnDefinition = "TEXT")
    private String extraConfig;

    private Boolean active = true;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static SttVendorConfig defaultConfig() {
        SttVendorConfig config = new SttVendorConfig();
        config.setVendorName(Constants.DEFAULT_VENDOR_NAME);
        config.setLanguageCode(Constants.DEFAULT_LANGUAGE_CODE);
        config.setStreamingEnabled(true);
        config.setActive(true);
        return config;
    }
}
