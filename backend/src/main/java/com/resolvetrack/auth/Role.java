package com.resolvetrack.auth;

public enum Role {
    ADMIN,
    SUPERVISOR,
    OFFICER,
    DATA_ENTRY,
    CITIZEN;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }

    public static Role fromString(String roleStr) {
        if (roleStr == null) {
            return CITIZEN;
        }
        String clean = roleStr.startsWith("ROLE_") ? roleStr.substring(5) : roleStr;
        for (Role r : Role.values()) {
            if (r.name().equalsIgnoreCase(clean)) {
                return r;
            }
        }
        return CITIZEN;
    }
}
