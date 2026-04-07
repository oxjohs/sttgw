package com.oxjohs.sttgw.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateUtils {

    public static final DateTimeFormatter DATE_PATH_FORMATTER =
        DateTimeFormatter.ofPattern(Constants.DATE_PATH_PATTERN);

    public static String formatDatePath(LocalDateTime dateTime) {
        return dateTime.format(DATE_PATH_FORMATTER);
    }
}
