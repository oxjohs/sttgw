CREATE TABLE IF NOT EXISTS CALL_RECORD (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    call_id VARCHAR(255) NOT NULL UNIQUE,
    caller_number VARCHAR(20),
    agent_extension VARCHAR(20),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_sec INT,
    state VARCHAR(20) NOT NULL DEFAULT 'CONNECTED',
    rx_audio_path VARCHAR(500),
    tx_audio_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_call_record_start_time ON CALL_RECORD(start_time DESC);
CREATE INDEX IF NOT EXISTS idx_call_record_caller ON CALL_RECORD(caller_number);
CREATE INDEX IF NOT EXISTS idx_call_record_state ON CALL_RECORD(state);

CREATE TABLE IF NOT EXISTS STT_TRANSCRIPT (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    call_id VARCHAR(255) NOT NULL,
    channel VARCHAR(2) NOT NULL,
    transcript TEXT NOT NULL,
    confidence FLOAT DEFAULT 0.0,
    is_final BOOLEAN DEFAULT TRUE,
    timestamp TIMESTAMP NOT NULL,
    vendor_name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transcript_call FOREIGN KEY (call_id) REFERENCES CALL_RECORD(call_id)
);

CREATE INDEX IF NOT EXISTS idx_transcript_call_id ON STT_TRANSCRIPT(call_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_transcript_channel ON STT_TRANSCRIPT(call_id, channel);

CREATE TABLE IF NOT EXISTS STT_VENDOR_CONFIG (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_name VARCHAR(50) NOT NULL,
    language_code VARCHAR(10) DEFAULT 'ko-KR',
    streaming_enabled BOOLEAN DEFAULT TRUE,
    credential_path VARCHAR(500),
    extra_config TEXT,
    active BOOLEAN DEFAULT TRUE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
