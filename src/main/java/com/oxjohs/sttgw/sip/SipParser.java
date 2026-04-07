package com.oxjohs.sttgw.sip;

import com.oxjohs.sttgw.session.CallSessionManager;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SipParser {

    private static final Pattern MEDIA_PATTERN = Pattern.compile("m=audio\\s+(\\d+)\\s+RTP/AVP\\s+(\\d+)");
    private static final Pattern CONNECTION_PATTERN = Pattern.compile("c=IN\\s+IP4\\s+([\\d.]+)");
    private static final Pattern URI_USER_PATTERN = Pattern.compile("sip:([^@>;]+)");
    private static final Pattern TAG_PATTERN = Pattern.compile("tag=([^;\\s]+)");

    public void parse(byte[] payload, CallSessionManager sessionManager) {
        String sipText = new String(payload, StandardCharsets.UTF_8);
        SipMessage message = parseSipMessage(sipText);
        if (message == null) {
            return;
        }

        switch (message.getMethod()) {
            case INVITE -> {
                log.info("SIP INVITE 수신: callId={}, from={}", message.getCallId(), message.getFromUri());
                sessionManager.onCallStart(message);
            }
            case BYE -> {
                log.info("SIP BYE 수신: callId={}", message.getCallId());
                sessionManager.onCallEnd(message);
            }
            case CANCEL -> {
                log.info("SIP CANCEL 수신: callId={}", message.getCallId());
                sessionManager.onCallCancel(message);
            }
            default -> log.debug("SIP {} 무시: callId={}", message.getMethod(), message.getCallId());
        }
    }

    SipMessage parseSipMessage(String sipText) {
        if (sipText == null || sipText.isBlank() || !sipText.contains("\r\n")) {
            return null;
        }

        String firstLine = sipText.substring(0, sipText.indexOf("\r\n"));
        SipMessage.SipMessageBuilder builder = SipMessage.builder()
            .method(SipMethodType.fromFirstLine(firstLine))
            .headers(extractHeaders(sipText));

        String callId = extractHeader(sipText, "Call-ID");
        String from = extractHeader(sipText, "From");
        String to = extractHeader(sipText, "To");

        builder.callId(callId);
        builder.fromUri(extractUriUser(from));
        builder.toUri(extractUriUser(to));
        builder.fromTag(extractTag(from));
        builder.toTag(extractTag(to));

        if (sipText.contains("v=0")) {
            parseSdp(sipText, builder);
        }

        return builder.build();
    }

    private void parseSdp(String sipText, SipMessage.SipMessageBuilder builder) {
        Matcher mediaMatcher = MEDIA_PATTERN.matcher(sipText);
        if (mediaMatcher.find()) {
            builder.mediaPort(Integer.parseInt(mediaMatcher.group(1)));
            builder.codec(mediaMatcher.group(2));
        }

        Matcher connectionMatcher = CONNECTION_PATTERN.matcher(sipText);
        if (connectionMatcher.find()) {
            builder.mediaIp(connectionMatcher.group(1));
        }
    }

    private Map<String, String> extractHeaders(String sipText) {
        Map<String, String> headers = new LinkedHashMap<>();
        String[] lines = sipText.split("\r\n");
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank()) {
                break;
            }

            int separatorIndex = line.indexOf(':');
            if (separatorIndex < 0) {
                continue;
            }

            String name = line.substring(0, separatorIndex).trim();
            String value = line.substring(separatorIndex + 1).trim();
            headers.put(name, value);
        }
        return headers;
    }

    private String extractHeader(String sipText, String headerName) {
        Pattern pattern = Pattern.compile("^" + Pattern.quote(headerName) + ":\\s*(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(sipText);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String extractUriUser(String headerValue) {
        Matcher matcher = URI_USER_PATTERN.matcher(headerValue);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String extractTag(String headerValue) {
        Matcher matcher = TAG_PATTERN.matcher(headerValue);
        return matcher.find() ? matcher.group(1) : "";
    }
}
