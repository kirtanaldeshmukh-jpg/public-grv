package com.resolvetrack.analytics;

import java.util.List;
import java.util.Map;

public class AnalyticsDto {

    public static class ExecutiveOverview {
        private long totalGrievances;
        private long activeGrievances;
        private long resolvedGrievances;
        private long escalatedGrievances;
        private double overallSlaComplianceRate;
        private List<DepartmentBenchmark> departmentBenchmarks;
        private Map<String, Long> statusDistribution;

        public ExecutiveOverview() {}

        public ExecutiveOverview(long totalGrievances, long activeGrievances, long resolvedGrievances, long escalatedGrievances, double overallSlaComplianceRate, List<DepartmentBenchmark> departmentBenchmarks, Map<String, Long> statusDistribution) {
            this.totalGrievances = totalGrievances;
            this.activeGrievances = activeGrievances;
            this.resolvedGrievances = resolvedGrievances;
            this.escalatedGrievances = escalatedGrievances;
            this.overallSlaComplianceRate = overallSlaComplianceRate;
            this.departmentBenchmarks = departmentBenchmarks;
            this.statusDistribution = statusDistribution;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private long totalGrievances;
            private long activeGrievances;
            private long resolvedGrievances;
            private long escalatedGrievances;
            private double overallSlaComplianceRate;
            private List<DepartmentBenchmark> departmentBenchmarks;
            private Map<String, Long> statusDistribution;

            public Builder totalGrievances(long total) { this.totalGrievances = total; return this; }
            public Builder activeGrievances(long active) { this.activeGrievances = active; return this; }
            public Builder resolvedGrievances(long res) { this.resolvedGrievances = res; return this; }
            public Builder escalatedGrievances(long esc) { this.escalatedGrievances = esc; return this; }
            public Builder overallSlaComplianceRate(double rate) { this.overallSlaComplianceRate = rate; return this; }
            public Builder departmentBenchmarks(List<DepartmentBenchmark> list) { this.departmentBenchmarks = list; return this; }
            public Builder statusDistribution(Map<String, Long> dist) { this.statusDistribution = dist; return this; }

            public ExecutiveOverview build() {
                return new ExecutiveOverview(totalGrievances, activeGrievances, resolvedGrievances, escalatedGrievances, overallSlaComplianceRate, departmentBenchmarks, statusDistribution);
            }
        }

        public long getTotalGrievances() { return totalGrievances; }
        public long getActiveGrievances() { return activeGrievances; }
        public long getResolvedGrievances() { return resolvedGrievances; }
        public long getEscalatedGrievances() { return escalatedGrievances; }
        public double getOverallSlaComplianceRate() { return overallSlaComplianceRate; }
        public List<DepartmentBenchmark> getDepartmentBenchmarks() { return departmentBenchmarks; }
        public Map<String, Long> getStatusDistribution() { return statusDistribution; }
    }

    public static class DepartmentBenchmark {
        private Long departmentId;
        private String departmentNameEn;
        private String departmentNameKn;
        private long totalComplaints;
        private long resolvedCount;
        private long overdueCount;
        private double slaCompliancePercentage;
        private double averageResolutionDays;

        public DepartmentBenchmark() {}

        public DepartmentBenchmark(Long departmentId, String departmentNameEn, String departmentNameKn, long totalComplaints, long resolvedCount, long overdueCount, double slaCompliancePercentage, double averageResolutionDays) {
            this.departmentId = departmentId;
            this.departmentNameEn = departmentNameEn;
            this.departmentNameKn = departmentNameKn;
            this.totalComplaints = totalComplaints;
            this.resolvedCount = resolvedCount;
            this.overdueCount = overdueCount;
            this.slaCompliancePercentage = slaCompliancePercentage;
            this.averageResolutionDays = averageResolutionDays;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private Long departmentId;
            private String departmentNameEn;
            private String departmentNameKn;
            private long totalComplaints;
            private long resolvedCount;
            private long overdueCount;
            private double slaCompliancePercentage;
            private double averageResolutionDays;

            public Builder departmentId(Long id) { this.departmentId = id; return this; }
            public Builder departmentNameEn(String name) { this.departmentNameEn = name; return this; }
            public Builder departmentNameKn(String name) { this.departmentNameKn = name; return this; }
            public Builder totalComplaints(long total) { this.totalComplaints = total; return this; }
            public Builder resolvedCount(long count) { this.resolvedCount = count; return this; }
            public Builder overdueCount(long count) { this.overdueCount = count; return this; }
            public Builder slaCompliancePercentage(double pct) { this.slaCompliancePercentage = pct; return this; }
            public Builder averageResolutionDays(double days) { this.averageResolutionDays = days; return this; }

            public DepartmentBenchmark build() {
                return new DepartmentBenchmark(departmentId, departmentNameEn, departmentNameKn, totalComplaints, resolvedCount, overdueCount, slaCompliancePercentage, averageResolutionDays);
            }
        }

        public Long getDepartmentId() { return departmentId; }
        public String getDepartmentNameEn() { return departmentNameEn; }
        public String getDepartmentNameKn() { return departmentNameKn; }
        public long getTotalComplaints() { return totalComplaints; }
        public long getResolvedCount() { return resolvedCount; }
        public long getOverdueCount() { return overdueCount; }
        public double getSlaCompliancePercentage() { return slaCompliancePercentage; }
        public double getAverageResolutionDays() { return averageResolutionDays; }
    }
}
