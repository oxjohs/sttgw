package com.oxjohs.sttgw.api.dto;

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
public class ApiErrorResponse {

    private String error;
    private String message;
    private LocalDateTime timestamp;

    public static ApiErrorResponse of(String error, String message) {
        return ApiErrorResponse.builder()
            .error(error)
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
