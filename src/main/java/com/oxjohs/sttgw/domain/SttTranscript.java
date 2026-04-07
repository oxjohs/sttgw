package com.oxjohs.sttgw.domain;

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
@Table(name = "STT_TRANSCRIPT")
public class SttTranscript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "call_id", nullable = false)
    private String callId;

    @Column(nullable = false, length = 2)
    private String channel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String transcript;

    private Float confidence = 0.0f;

    @Column(name = "is_final")
    private Boolean isFinal = true;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
