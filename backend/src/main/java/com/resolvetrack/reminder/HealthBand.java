package com.resolvetrack.reminder;

public enum HealthBand {
    HEALTHY,
    ATTENTION,
    CRITICAL;

    public static HealthBand fromScore(int score) {
        if (score >= 80) return HEALTHY;
        if (score >= 50) return ATTENTION;
        return CRITICAL;
    }
}

