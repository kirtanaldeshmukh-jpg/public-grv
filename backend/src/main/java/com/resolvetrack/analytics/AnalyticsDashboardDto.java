package com.resolvetrack.analytics;

import java.util.List;

public class AnalyticsDashboardDto {
    private double averageResolutionTimeHours;
    private double overduePercentage;
    private long pendingCount;
    private long escalationCount;
    private long reopenedCount;
    private long totalCount;
    private long resolvedCount;
    private List<OfficerWorkloadDto> officerWorkload;
    private List<DepartmentOverdueDto> overdueByDepartment;
    private List<AreaOverdueDto> overdueByArea;

    public AnalyticsDashboardDto() {}

    public AnalyticsDashboardDto(
            double averageResolutionTimeHours,
            double overduePercentage,
            long pendingCount,
            long escalationCount,
            long reopenedCount,
            long totalCount,
            long resolvedCount,
            List<OfficerWorkloadDto> officerWorkload,
            List<DepartmentOverdueDto> overdueByDepartment,
            List<AreaOverdueDto> overdueByArea) {
        this.averageResolutionTimeHours = averageResolutionTimeHours;
        this.overduePercentage = overduePercentage;
        this.pendingCount = pendingCount;
        this.escalationCount = escalationCount;
        this.reopenedCount = reopenedCount;
        this.totalCount = totalCount;
        this.resolvedCount = resolvedCount;
        this.officerWorkload = officerWorkload;
        this.overdueByDepartment = overdueByDepartment;
        this.overdueByArea = overdueByArea;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double averageResolutionTimeHours;
        private double overduePercentage;
        private long pendingCount;
        private long escalationCount;
        private long reopenedCount;
        private long totalCount;
        private long resolvedCount;
        private List<OfficerWorkloadDto> officerWorkload;
        private List<DepartmentOverdueDto> overdueByDepartment;
        private List<AreaOverdueDto> overdueByArea;

        public Builder averageResolutionTimeHours(double val) { this.averageResolutionTimeHours = val; return this; }
        public Builder overduePercentage(double val) { this.overduePercentage = val; return this; }
        public Builder pendingCount(long val) { this.pendingCount = val; return this; }
        public Builder escalationCount(long val) { this.escalationCount = val; return this; }
        public Builder reopenedCount(long val) { this.reopenedCount = val; return this; }
        public Builder totalCount(long val) { this.totalCount = val; return this; }
        public Builder resolvedCount(long val) { this.resolvedCount = val; return this; }
        public Builder officerWorkload(List<OfficerWorkloadDto> val) { this.officerWorkload = val; return this; }
        public Builder overdueByDepartment(List<DepartmentOverdueDto> val) { this.overdueByDepartment = val; return this; }
        public Builder overdueByArea(List<AreaOverdueDto> val) { this.overdueByArea = val; return this; }

        public AnalyticsDashboardDto build() {
            return new AnalyticsDashboardDto(
                    averageResolutionTimeHours,
                    overduePercentage,
                    pendingCount,
                    escalationCount,
                    reopenedCount,
                    totalCount,
                    resolvedCount,
                    officerWorkload,
                    overdueByDepartment,
                    overdueByArea
            );
        }
    }

    public double getAverageResolutionTimeHours() { return averageResolutionTimeHours; }
    public void setAverageResolutionTimeHours(double averageResolutionTimeHours) { this.averageResolutionTimeHours = averageResolutionTimeHours; }

    public double getOverduePercentage() { return overduePercentage; }
    public void setOverduePercentage(double overduePercentage) { this.overduePercentage = overduePercentage; }

    public long getPendingCount() { return pendingCount; }
    public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }

    public long getEscalationCount() { return escalationCount; }
    public void setEscalationCount(long escalationCount) { this.escalationCount = escalationCount; }

    public long getReopenedCount() { return reopenedCount; }
    public void setReopenedCount(long reopenedCount) { this.reopenedCount = reopenedCount; }

    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }

    public long getResolvedCount() { return resolvedCount; }
    public void setResolvedCount(long resolvedCount) { this.resolvedCount = resolvedCount; }

    public List<OfficerWorkloadDto> getOfficerWorkload() { return officerWorkload; }
    public void setOfficerWorkload(List<OfficerWorkloadDto> officerWorkload) { this.officerWorkload = officerWorkload; }

    public List<DepartmentOverdueDto> getOverdueByDepartment() { return overdueByDepartment; }
    public void setOverdueByDepartment(List<DepartmentOverdueDto> overdueByDepartment) { this.overdueByDepartment = overdueByDepartment; }

    public List<AreaOverdueDto> getOverdueByArea() { return overdueByArea; }
    public void setOverdueByArea(List<AreaOverdueDto> overdueByArea) { this.overdueByArea = overdueByArea; }

    public static class OfficerWorkloadDto {
        private Long officerId;
        private String officerName;
        private String employeeCode;
        private String departmentName;
        private long activeCount;
        private long overdueCount;
        private long resolvedCount;

        public OfficerWorkloadDto() {}

        public OfficerWorkloadDto(Long officerId, String officerName, String employeeCode, String departmentName, long activeCount, long overdueCount, long resolvedCount) {
            this.officerId = officerId;
            this.officerName = officerName;
            this.employeeCode = employeeCode;
            this.departmentName = departmentName;
            this.activeCount = activeCount;
            this.overdueCount = overdueCount;
            this.resolvedCount = resolvedCount;
        }

        public Long getOfficerId() { return officerId; }
        public void setOfficerId(Long officerId) { this.officerId = officerId; }
        public String getOfficerName() { return officerName; }
        public void setOfficerName(String officerName) { this.officerName = officerName; }
        public String getEmployeeCode() { return employeeCode; }
        public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public long getActiveCount() { return activeCount; }
        public void setActiveCount(long activeCount) { this.activeCount = activeCount; }
        public long getOverdueCount() { return overdueCount; }
        public void setOverdueCount(long overdueCount) { this.overdueCount = overdueCount; }
        public long getResolvedCount() { return resolvedCount; }
        public void setResolvedCount(long resolvedCount) { this.resolvedCount = resolvedCount; }
    }

    public static class DepartmentOverdueDto {
        private Long departmentId;
        private String departmentNameEn;
        private String departmentNameKn;
        private String departmentCode;
        private long overdueCount;
        private long totalCount;
        private double overduePercentage;

        public DepartmentOverdueDto() {}

        public DepartmentOverdueDto(Long departmentId, String departmentNameEn, String departmentNameKn, String departmentCode, long overdueCount, long totalCount, double overduePercentage) {
            this.departmentId = departmentId;
            this.departmentNameEn = departmentNameEn;
            this.departmentNameKn = departmentNameKn;
            this.departmentCode = departmentCode;
            this.overdueCount = overdueCount;
            this.totalCount = totalCount;
            this.overduePercentage = overduePercentage;
        }

        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public String getDepartmentNameEn() { return departmentNameEn; }
        public void setDepartmentNameEn(String departmentNameEn) { this.departmentNameEn = departmentNameEn; }
        public String getDepartmentNameKn() { return departmentNameKn; }
        public void setDepartmentNameKn(String departmentNameKn) { this.departmentNameKn = departmentNameKn; }
        public String getDepartmentCode() { return departmentCode; }
        public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
        public long getOverdueCount() { return overdueCount; }
        public void setOverdueCount(long overdueCount) { this.overdueCount = overdueCount; }
        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
        public double getOverduePercentage() { return overduePercentage; }
        public void setOverduePercentage(double overduePercentage) { this.overduePercentage = overduePercentage; }
    }

    public static class AreaOverdueDto {
        private String areaName;
        private double latitude;
        private double longitude;
        private long complaintCount;
        private long overdueCount;
        private long criticalCount;
        private int radiusMeters;

        public AreaOverdueDto() {}

        public AreaOverdueDto(String areaName, double latitude, double longitude, long complaintCount, long overdueCount, long criticalCount, int radiusMeters) {
            this.areaName = areaName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.complaintCount = complaintCount;
            this.overdueCount = overdueCount;
            this.criticalCount = criticalCount;
            this.radiusMeters = radiusMeters;
        }

        public String getAreaName() { return areaName; }
        public void setAreaName(String areaName) { this.areaName = areaName; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public long getComplaintCount() { return complaintCount; }
        public void setComplaintCount(long complaintCount) { this.complaintCount = complaintCount; }
        public long getOverdueCount() { return overdueCount; }
        public void setOverdueCount(long overdueCount) { this.overdueCount = overdueCount; }
        public long getCriticalCount() { return criticalCount; }
        public void setCriticalCount(long criticalCount) { this.criticalCount = criticalCount; }
        public int getRadiusMeters() { return radiusMeters; }
        public void setRadiusMeters(int radiusMeters) { this.radiusMeters = radiusMeters; }
    }
}

