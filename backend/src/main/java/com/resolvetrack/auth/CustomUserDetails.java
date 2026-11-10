package com.resolvetrack.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@SuppressWarnings("serial")
public class CustomUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private final String password;
    private final String fullName;
    private final Role role;
    private final Long departmentId;
    private final Long officerId;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long userId,
                             String username,
                             String password,
                             Role role,
                             Long departmentId,
                             Long officerId,
                             boolean enabled,
                             Collection<? extends GrantedAuthority> authorities) {
        this(userId, username, password, username, role, departmentId, officerId, enabled, authorities);
    }

    public CustomUserDetails(User user) {
        this(
                user != null ? user.getId() : 1L,
                user != null ? user.getUsername() : "user",
                user != null && user.getPasswordHash() != null ? user.getPasswordHash() : "",
                user != null && user.getFullName() != null ? user.getFullName() : "User",
                user != null && user.getRole() != null ? user.getRole() : Role.CITIZEN,
                user != null ? user.getDepartmentId() : null,
                null,
                user == null || user.isEnabled()
        );
    }

    public CustomUserDetails(Long userId,
                             String username,
                             String password,
                             String fullName,
                             Role role,
                             Long departmentId,
                             Long officerId,
                             boolean enabled) {
        this(userId, username, password, fullName, role, departmentId, officerId, enabled,
                java.util.List.of(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(role != null ? role.name() : "USER"),
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(role != null ? "ROLE_" + role.name() : "ROLE_USER")
                ));
    }

    public CustomUserDetails(Long userId,
                             String username,
                             String password,
                             String fullName,
                             Role role,
                             Long departmentId,
                             Long officerId,
                             boolean enabled,
                             Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.departmentId = departmentId;
        this.officerId = officerId;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    public String getFullName() {
        return fullName;
    }

    public Long getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public Long getOfficerId() {
        return officerId;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isSupervisor() {
        return role == Role.SUPERVISOR;
    }

    public boolean isOfficer() {
        return role == Role.OFFICER;
    }

    public boolean isDataEntry() {
        return role == Role.DATA_ENTRY;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

