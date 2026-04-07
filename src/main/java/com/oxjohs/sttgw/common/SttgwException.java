package com.oxjohs.sttgw.common;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SttgwException extends RuntimeException {

    public SttgwException(String message) {
        super(message);
    }

    public SttgwException(String message, Throwable cause) {
        super(message, cause);
    }
}
