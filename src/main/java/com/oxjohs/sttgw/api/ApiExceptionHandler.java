package com.oxjohs.sttgw.api;

import com.oxjohs.sttgw.api.dto.ApiErrorResponse;
import com.oxjohs.sttgw.common.SttgwException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(SttgwException.class)
    public ResponseEntity<ApiErrorResponse> handleSttgwException(SttgwException exception) {
        return ResponseEntity.badRequest()
            .body(ApiErrorResponse.of("STTGW_ERROR", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException exception
    ) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError == null ? "요청 값이 올바르지 않습니다" : fieldError.getDefaultMessage();
        return ResponseEntity.badRequest()
            .body(ApiErrorResponse.of("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
        ConstraintViolationException exception
    ) {
        return ResponseEntity.badRequest()
            .body(ApiErrorResponse.of("VALIDATION_ERROR", exception.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFoundException(NoResourceFoundException exception) {
        if ("favicon.ico".equals(exception.getResourcePath())) {
            log.debug("favicon 리소스 요청 무시");
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        log.warn("정적 리소스를 찾을 수 없습니다: path={}", exception.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiErrorResponse.of("RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception exception) {
        log.error("처리되지 않은 API 오류", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiErrorResponse.of("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다"));
    }
}
