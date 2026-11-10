package com.resolvetrack.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDto {

    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;

        public LoginRequest() {}
        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50)
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        private String email;

        @NotBlank(message = "Full name is required")
        private String fullName;

        private String phoneNumber;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        private Role role;

        private Long departmentId;

        public RegisterRequest() {}

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    }

    public static class AuthResponse {
        private String token;
        private String tokenType;
        private Long userId;
        private String username;
        private String fullName;
        private String email;
        private Role role;
        private Long departmentId;
        private Long officerId;

        public AuthResponse() {}
        public AuthResponse(String token, String tokenType, Long userId, String username, String fullName, String email, Role role, Long departmentId, Long officerId) {
            this.token = token;
            this.tokenType = tokenType;
            this.userId = userId;
            this.username = username;
            this.fullName = fullName;
            this.email = email;
            this.role = role;
            this.departmentId = departmentId;
            this.officerId = officerId;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private String token;
            private String tokenType;
            private Long userId;
            private String username;
            private String fullName;
            private String email;
            private Role role;
            private Long departmentId;
            private Long officerId;

            public Builder token(String token) { this.token = token; return this; }
            public Builder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
            public Builder userId(Long userId) { this.userId = userId; return this; }
            public Builder username(String username) { this.username = username; return this; }
            public Builder fullName(String fullName) { this.fullName = fullName; return this; }
            public Builder email(String email) { this.email = email; return this; }
            public Builder role(Role role) { this.role = role; return this; }
            public Builder departmentId(Long departmentId) { this.departmentId = departmentId; return this; }
            public Builder officerId(Long officerId) { this.officerId = officerId; return this; }

            public AuthResponse build() {
                return new AuthResponse(token, tokenType, userId, username, fullName, email, role, departmentId, officerId);
            }
        }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public Long getOfficerId() { return officerId; }
        public void setOfficerId(Long officerId) { this.officerId = officerId; }
    }
}
