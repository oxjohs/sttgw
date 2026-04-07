package com.oxjohs.sttgw.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

    public static final String DEFAULT_VENDOR_NAME = "GOOGLE";
    public static final String DEFAULT_LANGUAGE_CODE = "ko-KR";
    public static final String CHANNEL_RX = "RX";
    public static final String CHANNEL_TX = "TX";
    public static final String DATE_PATH_PATTERN = "yyyy/MM/dd";
}
