package com.resolvetrack.complaint;

public enum ComplaintStatus {
    NEW,
    ASSIGNED,
    TRIAGED,
    IN_PROGRESS,
    RESOLVED,
    REJECTED,
    ESCALATED,
    OVERDUE,
    SUBMITTED,
    RESOLUTION_SUBMITTED,
    REOPENED;

    public boolean isResolved() {
        return this == RESOLVED || this == REJECTED;
    }

    public boolean isPending() {
        return !isResolved();
    }
}
