package com.resolvetrack.complaint;

public enum ComplaintSource {
    WEB,
    PHONE,
    PAPER;

    public static ComplaintSource fromString(String sourceStr) {
        if (sourceStr == null || sourceStr.isBlank()) {
            return WEB;
        }
        for (ComplaintSource s : ComplaintSource.values()) {
            if (s.name().equalsIgnoreCase(sourceStr.trim())) {
                return s;
            }
        }
        return WEB;
    }
}

