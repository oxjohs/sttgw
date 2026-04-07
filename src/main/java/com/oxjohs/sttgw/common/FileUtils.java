package com.oxjohs.sttgw.common;

import java.nio.file.Path;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileUtils {

    public static Path resolveDatedDirectory(String basePath, LocalDateTime dateTime) {
        return Path.of(basePath, DateUtils.formatDatePath(dateTime));
    }

    public static String sanitizeFileName(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
