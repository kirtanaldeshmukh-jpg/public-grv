package com.resolvetrack.department;

import java.util.List;

public interface DepartmentSuggestionService {

    SuggestionResult suggestDepartment(String category, String text);

    default SuggestionResult suggest(String category, String title, String description, String locationAddress) {
        String text = (title != null ? title : "") + " " + (description != null ? description : "") + " " + (locationAddress != null ? locationAddress : "");
        return suggestDepartment(category, text.trim());
    }

    class SuggestionResult {
        private Long departmentId;
        private String departmentCode;
        private String departmentNameEn;
        private String departmentNameKn;
        private double confidence;
        private List<String> matchedKeywords;
        private String explanation;
        private String priority;
        private String issueSummary;
        private String extractedLocation;
        private String category;
        private String source; // "AI" or "RULE_BASED"

        public SuggestionResult() {}

        public SuggestionResult(
                Long departmentId,
                String departmentCode,
                String departmentNameEn,
                String departmentNameKn,
                double confidence,
                List<String> matchedKeywords,
                String explanation,
                String priority,
                String issueSummary,
                String extractedLocation,
                String category,
                String source) {
            this.departmentId = departmentId;
            this.departmentCode = departmentCode;
            this.departmentNameEn = departmentNameEn;
            this.departmentNameKn = departmentNameKn;
            this.confidence = confidence;
            this.matchedKeywords = matchedKeywords;
            this.explanation = explanation;
            this.priority = priority;
            this.issueSummary = issueSummary;
            this.extractedLocation = extractedLocation;
            this.category = category;
            this.source = source;
        }

        public SuggestionResult(
                Long departmentId,
                String departmentCode,
                String departmentNameEn,
                String departmentNameKn,
                double confidence,
                List<String> matchedKeywords,
                String explanation) {
            this(departmentId, departmentCode, departmentNameEn, departmentNameKn, confidence, matchedKeywords, explanation, "MEDIUM", null, null, null, "RULE_BASED");
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Long departmentId;
            private String departmentCode;
            private String departmentNameEn;
            private String departmentNameKn;
            private double confidence;
            private List<String> matchedKeywords;
            private String explanation;
            private String priority = "MEDIUM";
            private String issueSummary;
            private String extractedLocation;
            private String category;
            private String source = "RULE_BASED";

            public Builder departmentId(Long departmentId) { this.departmentId = departmentId; return this; }
            public Builder departmentCode(String departmentCode) { this.departmentCode = departmentCode; return this; }
            public Builder departmentNameEn(String departmentNameEn) { this.departmentNameEn = departmentNameEn; return this; }
            public Builder departmentNameKn(String departmentNameKn) { this.departmentNameKn = departmentNameKn; return this; }
            public Builder confidence(double confidence) { this.confidence = confidence; return this; }
            public Builder matchedKeywords(List<String> matchedKeywords) { this.matchedKeywords = matchedKeywords; return this; }
            public Builder explanation(String explanation) { this.explanation = explanation; return this; }
            public Builder priority(String priority) { this.priority = priority; return this; }
            public Builder issueSummary(String issueSummary) { this.issueSummary = issueSummary; return this; }
            public Builder extractedLocation(String extractedLocation) { this.extractedLocation = extractedLocation; return this; }
            public Builder category(String category) { this.category = category; return this; }
            public Builder source(String source) { this.source = source; return this; }

            public SuggestionResult build() {
                return new SuggestionResult(
                        departmentId, departmentCode, departmentNameEn, departmentNameKn,
                        confidence, matchedKeywords, explanation, priority, issueSummary,
                        extractedLocation, category, source
                );
            }
        }

        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public String getDepartmentCode() { return departmentCode; }
        public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
        public String getDepartmentNameEn() { return departmentNameEn; }
        public void setDepartmentNameEn(String departmentNameEn) { this.departmentNameEn = departmentNameEn; }
        public String getDepartmentNameKn() { return departmentNameKn; }
        public void setDepartmentNameKn(String departmentNameKn) { this.departmentNameKn = departmentNameKn; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public List<String> getMatchedKeywords() { return matchedKeywords; }
        public void setMatchedKeywords(List<String> matchedKeywords) { this.matchedKeywords = matchedKeywords; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getIssueSummary() { return issueSummary; }
        public void setIssueSummary(String issueSummary) { this.issueSummary = issueSummary; }
        public String getExtractedLocation() { return extractedLocation; }
        public void setExtractedLocation(String extractedLocation) { this.extractedLocation = extractedLocation; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }
}
