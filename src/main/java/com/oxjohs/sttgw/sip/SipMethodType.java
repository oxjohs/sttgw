package com.oxjohs.sttgw.sip;

public enum SipMethodType {
    INVITE,
    BYE,
    ACK,
    CANCEL,
    OPTIONS,
    REGISTER,
    UNKNOWN;

    public static SipMethodType fromFirstLine(String firstLine) {
        if (firstLine == null || firstLine.isBlank()) {
            return UNKNOWN;
        }

        String methodToken = firstLine.trim().split("\\s+")[0].toUpperCase();
        return switch (methodToken) {
            case "INVITE" -> INVITE;
            case "BYE" -> BYE;
            case "ACK" -> ACK;
            case "CANCEL" -> CANCEL;
            case "OPTIONS" -> OPTIONS;
            case "REGISTER" -> REGISTER;
            default -> UNKNOWN;
        };
    }
}
