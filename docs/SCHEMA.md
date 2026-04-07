# sttgw — 데이터베이스 스키마 설계

## 1. 개요

- **DBMS**: H2 Database (파일 모드)
- **파일 위치**: `/APP_DATA/db/sttgw`
- **접속 URL**: `jdbc:h2:file:/APP_DATA/db/sttgw;AUTO_SERVER=TRUE`
- **콘솔**: `http://localhost:8080/h2-console`
- **DDL 관리**: `src/main/resources/schema.sql` + JPA `ddl-auto: update`

## 2. ER 다이어그램

```
┌────────────────────┐       ┌─────────────────────────┐
│    CALL_RECORD     │       │     STT_TRANSCRIPT      │
├────────────────────┤       ├─────────────────────────┤
│ PK id (BIGINT)     │──1:N─▶│ PK id (BIGINT)          │
│    call_id (UNIQ)  │       │ FK call_id              │
│    caller_number   │       │    channel (RX/TX)      │
│    agent_extension │       │    transcript           │
│    start_time      │       │    confidence           │
│    end_time        │       │    is_final             │
│    duration_sec    │       │    timestamp            │
│    state           │       │    vendor_name          │
│    rx_audio_path   │       │    created_at           │
│    tx_audio_path   │       └─────────────────────────┘
│    created_at      │
└────────────────────┘

┌─────────────────────────┐
│   STT_VENDOR_CONFIG     │
├─────────────────────────┤
│ PK id (BIGINT)          │
│    vendor_name          │
│    language_code        │
│    streaming_enabled    │
│    credential_path      │
│    extra_config (JSON)  │
│    active               │
│    updated_at           │
└─────────────────────────┘
```

## 3. 테이블 정의

### 3.1 CALL_RECORD — 통화 이력

통화 1건에 대한 메타데이터를 저장합니다.

```sql
CREATE TABLE IF NOT EXISTS CALL_RECORD (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    call_id         VARCHAR(255) NOT NULL UNIQUE,  -- SIP Call-ID
    caller_number   VARCHAR(20),                    -- 고객 전화번호 (From)
    agent_extension VARCHAR(20),                    -- 상담사 내선번호 (To)
    start_time      TIMESTAMP NOT NULL,
    end_time        TIMESTAMP,
    duration_sec    INT,                            -- 통화 시간 (초)
    state           VARCHAR(20) NOT NULL DEFAULT 'CONNECTED',  -- CONNECTED, COMPLETED, CANCELLED
    rx_audio_path   VARCHAR(500),                   -- 고객 음성 WAV 파일 경로
    tx_audio_path   VARCHAR(500),                   -- 상담사 음성 WAV 파일 경로
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_call_record_start_time ON CALL_RECORD(start_time DESC);
CREATE INDEX idx_call_record_caller ON CALL_RECORD(caller_number);
CREATE INDEX idx_call_record_state ON CALL_RECORD(state);
```

**JPA 엔티티**:
```java
@Entity
@Table(name = "CALL_RECORD")
@Data
public class CallRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String callId;

    private String callerNumber;
    private String agentExtension;

    @Column(nullable = false)
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationSec;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallState state = CallState.CONNECTED;

    private String rxAudioPath;
    private String txAudioPath;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

### 3.2 STT_TRANSCRIPT — STT 변환 결과

통화 중 생성된 텍스트 결과를 시간순으로 저장합니다. 스트리밍 중간 결과(`is_final=false`)도 저장하여 추후 분석에 활용합니다.

```sql
CREATE TABLE IF NOT EXISTS STT_TRANSCRIPT (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    call_id         VARCHAR(255) NOT NULL,          -- CALL_RECORD.call_id 참조
    channel         VARCHAR(2) NOT NULL,            -- 'RX' (고객) 또는 'TX' (상담사)
    transcript      TEXT NOT NULL,                   -- 변환된 텍스트
    confidence      FLOAT DEFAULT 0.0,              -- 신뢰도 (0.0 ~ 1.0)
    is_final        BOOLEAN DEFAULT TRUE,           -- 최종 결과 여부
    timestamp       TIMESTAMP NOT NULL,             -- 발화 시각
    vendor_name     VARCHAR(50),                    -- 사용된 STT 벤더 (GOOGLE, NAVER 등)
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transcript_call FOREIGN KEY (call_id) REFERENCES CALL_RECORD(call_id)
);

CREATE INDEX idx_transcript_call_id ON STT_TRANSCRIPT(call_id, timestamp);
CREATE INDEX idx_transcript_channel ON STT_TRANSCRIPT(call_id, channel);
```

**JPA 엔티티**:
```java
@Entity
@Table(name = "STT_TRANSCRIPT")
@Data
public class SttTranscript {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String callId;

    @Column(nullable = false, length = 2)
    private String channel;  // "RX" or "TX"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String transcript;

    private Float confidence;
    private Boolean isFinal = true;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private String vendorName;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

### 3.3 STT_VENDOR_CONFIG — STT 벤더 설정

대시보드에서 변경 가능한 STT 벤더 설정을 저장합니다.

```sql
CREATE TABLE IF NOT EXISTS STT_VENDOR_CONFIG (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_name         VARCHAR(50) NOT NULL,       -- GOOGLE, NAVER, CUSTOM 등
    language_code       VARCHAR(10) DEFAULT 'ko-KR',
    streaming_enabled   BOOLEAN DEFAULT TRUE,
    credential_path     VARCHAR(500),               -- 인증키 파일 경로
    extra_config        TEXT,                        -- JSON 형태의 추가 설정
    active              BOOLEAN DEFAULT TRUE,        -- 현재 활성 벤더 여부
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**JPA 엔티티**:
```java
@Entity
@Table(name = "STT_VENDOR_CONFIG")
@Data
public class SttVendorConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String vendorName;

    private String languageCode = "ko-KR";
    private Boolean streamingEnabled = true;
    private String credentialPath;

    @Column(columnDefinition = "TEXT")
    private String extraConfig;

    private Boolean active = true;
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static SttVendorConfig defaultConfig() {
        SttVendorConfig c = new SttVendorConfig();
        c.setVendorName("GOOGLE");
        c.setLanguageCode("ko-KR");
        c.setStreamingEnabled(true);
        return c;
    }
}
```

## 4. 초기 데이터 (data.sql)

```sql
-- 기본 STT 벤더 설정 (Google STT)
INSERT INTO STT_VENDOR_CONFIG (vendor_name, language_code, streaming_enabled, credential_path, active, updated_at)
VALUES ('GOOGLE', 'ko-KR', TRUE, '/APP/sttgw/credentials/google-stt-key.json', TRUE, CURRENT_TIMESTAMP);
```

## 5. Repository 인터페이스

```java
public interface CallRecordRepository extends JpaRepository<CallRecord, Long> {
    Optional<CallRecord> findByCallId(String callId);
    Page<CallRecord> findAllByOrderByStartTimeDesc(Pageable pageable);
    List<CallRecord> findByState(CallState state);
    long countByStartTimeBetween(LocalDateTime from, LocalDateTime to);
}

public interface SttTranscriptRepository extends JpaRepository<SttTranscript, Long> {
    List<SttTranscript> findByCallIdOrderByTimestampAsc(String callId);
    List<SttTranscript> findByCallIdAndChannel(String callId, String channel);
    List<SttTranscript> findByCallIdAndIsFinalTrue(String callId);
}

public interface SttVendorConfigRepository extends JpaRepository<SttVendorConfig, Long> {
    Optional<SttVendorConfig> findFirstByOrderByIdAsc();
    Optional<SttVendorConfig> findByVendorNameAndActiveTrue(String vendorName);
}
```

## 6. 데이터 보존 정책

| 데이터 | 보존 기간 | 정리 방식 |
|--------|-----------|-----------|
| CALL_RECORD | 1년 | 스케줄러로 1년 경과 데이터 삭제 |
| STT_TRANSCRIPT (is_final=true) | 1년 | CALL_RECORD와 함께 삭제 |
| STT_TRANSCRIPT (is_final=false) | 7일 | 스케줄러로 7일 경과 중간 결과 삭제 |
| 오디오 파일 | 30일 | 스케줄러로 30일 경과 파일 삭제 (`sttgw.audio.retention-days`) |
| pcap 덤프 | 7일 | 스케줄러로 7일 경과 파일 삭제 |
| H2 DB 파일 | 영구 | 수동 백업 |
