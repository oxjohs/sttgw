package com.oxjohs.sttgw.stt;

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
public class SttResult {

    private String vendorName;
    private String transcript;
    private Float confidence;
    private boolean finalResult;
    private LocalDateTime timestamp;
    private SttMode mode;
    private String channel;

    public boolean isFinal() {
        return finalResult;
    }
}
