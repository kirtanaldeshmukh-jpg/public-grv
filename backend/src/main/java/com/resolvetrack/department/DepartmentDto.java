package com.resolvetrack.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DepartmentDto {

    public static class Response {
        private Long id;
        private String code;
        private String nameEn;
        private String nameKn;
        private String description;
        private Integer defaultSlaDays;
        private Integer escalationHours;
        private boolean active;

        public Response() {}

        public Response(Long id, String code, String nameEn, String nameKn, String description, Integer defaultSlaDays, Integer escalationHours, boolean active) {
            this.id = id;
            this.code = code;
            this.nameEn = nameEn;
            this.nameKn = nameKn;
            this.description = description;
            this.defaultSlaDays = defaultSlaDays;
            this.escalationHours = escalationHours;
            this.active = active;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private Long id;
            private String code;
            private String nameEn;
            private String nameKn;
            private String description;
            private Integer defaultSlaDays;
            private Integer escalationHours;
            private boolean active;

            public Builder id(Long id) { this.id = id; return this; }
            public Builder code(String code) { this.code = code; return this; }
            public Builder nameEn(String nameEn) { this.nameEn = nameEn; return this; }
            public Builder nameKn(String nameKn) { this.nameKn = nameKn; return this; }
            public Builder description(String description) { this.description = description; return this; }
            public Builder defaultSlaDays(Integer defaultSlaDays) { this.defaultSlaDays = defaultSlaDays; return this; }
            public Builder escalationHours(Integer escalationHours) { this.escalationHours = escalationHours; return this; }
            public Builder active(boolean active) { this.active = active; return this; }

            public Response build() {
                return new Response(id, code, nameEn, nameKn, description, defaultSlaDays, escalationHours, active);
            }
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getNameEn() { return nameEn; }
        public void setNameEn(String nameEn) { this.nameEn = nameEn; }
        public String getNameKn() { return nameKn; }
        public void setNameKn(String nameKn) { this.nameKn = nameKn; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getDefaultSlaDays() { return defaultSlaDays; }
        public void setDefaultSlaDays(Integer defaultSlaDays) { this.defaultSlaDays = defaultSlaDays; }
        public Integer getEscalationHours() { return escalationHours; }
        public void setEscalationHours(Integer escalationHours) { this.escalationHours = escalationHours; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public static class Request {
        @NotBlank(message = "Department code is required")
        private String code;

        @NotBlank(message = "English name is required")
        private String nameEn;

        @NotBlank(message = "Kannada name is required")
        private String nameKn;

        private String description;

        @NotNull(message = "SLA days is required")
        private Integer defaultSlaDays;

        @NotNull(message = "Escalation hours is required")
        private Integer escalationHours;

        public Request() {}

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getNameEn() { return nameEn; }
        public void setNameEn(String nameEn) { this.nameEn = nameEn; }
        public String getNameKn() { return nameKn; }
        public void setNameKn(String nameKn) { this.nameKn = nameKn; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getDefaultSlaDays() { return defaultSlaDays; }
        public void setDefaultSlaDays(Integer defaultSlaDays) { this.defaultSlaDays = defaultSlaDays; }
        public Integer getEscalationHours() { return escalationHours; }
        public void setEscalationHours(Integer escalationHours) { this.escalationHours = escalationHours; }
    }
}
