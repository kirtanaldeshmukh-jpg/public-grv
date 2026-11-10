package com.resolvetrack.reminder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HealthScoreResult {
    private final int score;
    private final HealthBand band;
    private final List<String> reasons;
    private final List<ReasonItem> reasonItems;

    public HealthScoreResult(int score, HealthBand band, List<String> reasons, List<ReasonItem> reasonItems) {
        this.score = score;
        this.band = band;
        this.reasons = reasons != null ? Collections.unmodifiableList(new ArrayList<>(reasons)) : Collections.emptyList();
        this.reasonItems = reasonItems != null ? Collections.unmodifiableList(new ArrayList<>(reasonItems)) : Collections.emptyList();
    }

    public HealthScoreResult(int score, HealthBand band, List<String> reasons) {
        this(score, band, reasons, convertStringsToItems(reasons));
    }

    private static List<ReasonItem> convertStringsToItems(List<String> list) {
        if (list == null) return Collections.emptyList();
        List<ReasonItem> items = new ArrayList<>();
        for (String s : list) {
            items.add(new ReasonItem("health.reason.custom", Collections.emptyMap(), s));
        }
        return items;
    }

    public int getScore() { return score; }
    public HealthBand getBand() { return band; }
    public List<String> getReasons() { return reasons; }
    public List<ReasonItem> getReasonItems() { return reasonItems; }

    @Override
    public String toString() {
        return "HealthScoreResult{score=" + score + ", band=" + band + ", reasons=" + reasons + '}';
    }

    public static class ReasonItem {
        private final String key;
        private final Map<String, Object> params;
        private final String message;

        public ReasonItem(String key, Map<String, Object> params, String message) {
            this.key = key;
            this.params = params != null ? params : Collections.emptyMap();
            this.message = message;
        }

        public String getKey() { return key; }
        public Map<String, Object> getParams() { return params; }
        public String getMessage() { return message; }
    }
}
