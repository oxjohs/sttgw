INSERT INTO STT_VENDOR_CONFIG (vendor_name, language_code, streaming_enabled, credential_path, active, updated_at)
SELECT 'GOOGLE', 'ko-KR', TRUE, '/APP/sttgw/credentials/google-stt-key.json', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM STT_VENDOR_CONFIG
    WHERE vendor_name = 'GOOGLE' AND active = TRUE
);
