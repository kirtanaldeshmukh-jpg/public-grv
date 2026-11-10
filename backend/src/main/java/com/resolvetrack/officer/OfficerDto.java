package com.resolvetrack.officer;

public class OfficerDto {

    public static class Response {
        private Long id;
        private Long userId;
        private String fullName;
        private String email;
        private String employeeCode;
        private Long departmentId;
        private String departmentNameEn;
        private String departmentNameKn;
        private String designation;
        private String jurisdictionZone;
        private Integer activeComplaintsCount;

        public Response() {}

        public Response(Long id, Long userId, String fullName, String email, String employeeCode, Long departmentId, String departmentNameEn, String departmentNameKn, String designation, String jurisdictionZone, Integer activeComplaintsCount) {
            this.id = id;
            this.userId = userId;
            this.fullName = fullName;
            this.email = email;
            this.employeeCode = employeeCode;
            this.departmentId = departmentId;
            this.departmentNameEn = departmentNameEn;
            this.departmentNameKn = departmentNameKn;
            this.designation = designation;
            this.jurisdictionZone = jurisdictionZone;
            this.activeComplaintsCount = activeComplaintsCount;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private Long id;
            private Long userId;
            private String fullName;
            private String email;
            private String employeeCode;
            private Long departmentId;
            private String departmentNameEn;
            private String departmentNameKn;
            private String designation;
            private String jurisdictionZone;
            private Integer activeComplaintsCount;

            public Builder id(Long id) { this.id = id; return this; }
            public Builder userId(Long userId) { this.userId = userId; return this; }
            public Builder fullName(String fullName) { this.fullName = fullName; return this; }
            public Builder email(String email) { this.email = email; return this; }
            public Builder employeeCode(String employeeCode) { this.employeeCode = employeeCode; return this; }
            public Builder departmentId(Long departmentId) { this.departmentId = departmentId; return this; }
            public Builder departmentNameEn(String departmentNameEn) { this.departmentNameEn = departmentNameEn; return this; }
            public Builder departmentNameKn(String departmentNameKn) { this.departmentNameKn = departmentNameKn; return this; }
            public Builder designation(String designation) { this.designation = designation; return this; }
            public Builder jurisdictionZone(String jurisdictionZone) { this.jurisdictionZone = jurisdictionZone; return this; }
            public Builder activeComplaintsCount(Integer count) { this.activeComplaintsCount = count; return this; }

            public Response build() {
                return new Response(id, userId, fullName, email, employeeCode, departmentId, departmentNameEn, departmentNameKn, designation, jurisdictionZone, activeComplaintsCount);
            }
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getEmployeeCode() { return employeeCode; }
        public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public String getDepartmentNameEn() { return departmentNameEn; }
        public void setDepartmentNameEn(String departmentNameEn) { this.departmentNameEn = departmentNameEn; }
        public String getDepartmentNameKn() { return departmentNameKn; }
        public void setDepartmentNameKn(String departmentNameKn) { this.departmentNameKn = departmentNameKn; }
        public String getDesignation() { return designation; }
        public void setDesignation(String designation) { this.designation = designation; }
        public String getJurisdictionZone() { return jurisdictionZone; }
        public void setJurisdictionZone(String jurisdictionZone) { this.jurisdictionZone = jurisdictionZone; }
        public Integer getActiveComplaintsCount() { return activeComplaintsCount; }
        public void setActiveComplaintsCount(Integer activeComplaintsCount) { this.activeComplaintsCount = activeComplaintsCount; }
    }
}
