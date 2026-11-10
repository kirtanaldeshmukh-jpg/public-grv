package com.resolvetrack.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DashboardSummaryDto {

    private long total;

    @JsonProperty("new")
    private long newCount;

    private long assigned;

    @JsonProperty("inProgress")
    private long inProgress;

    private long pending;
    private long overdue;
    private long resolved;

    public DashboardSummaryDto() {}

    public DashboardSummaryDto(long total, long newCount, long assigned, long inProgress, long pending, long overdue, long resolved) {
        this.total = total;
        this.newCount = newCount;
        this.assigned = assigned;
        this.inProgress = inProgress;
        this.pending = pending;
        this.overdue = overdue;
        this.resolved = resolved;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long total;
        private long newCount;
        private long assigned;
        private long inProgress;
        private long pending;
        private long overdue;
        private long resolved;

        public Builder total(long total) {
            this.total = total;
            return this;
        }

        public Builder newCount(long newCount) {
            this.newCount = newCount;
            return this;
        }

        public Builder assigned(long assigned) {
            this.assigned = assigned;
            return this;
        }

        public Builder inProgress(long inProgress) {
            this.inProgress = inProgress;
            return this;
        }

        public Builder pending(long pending) {
            this.pending = pending;
            return this;
        }

        public Builder overdue(long overdue) {
            this.overdue = overdue;
            return this;
        }

        public Builder resolved(long resolved) {
            this.resolved = resolved;
            return this;
        }

        public DashboardSummaryDto build() {
            return new DashboardSummaryDto(total, newCount, assigned, inProgress, pending, overdue, resolved);
        }
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    @JsonProperty("new")
    public long getNew() {
        return newCount;
    }

    public void setNew(long newCount) {
        this.newCount = newCount;
    }

    public long getNewCount() {
        return newCount;
    }

    public void setNewCount(long newCount) {
        this.newCount = newCount;
    }

    public long getAssigned() {
        return assigned;
    }

    public void setAssigned(long assigned) {
        this.assigned = assigned;
    }

    @JsonProperty("inProgress")
    public long getInProgress() {
        return inProgress;
    }

    public void setInProgress(long inProgress) {
        this.inProgress = inProgress;
    }

    @JsonProperty("in_progress")
    public long getInProgressSnake() {
        return inProgress;
    }

    public long getPending() {
        return pending;
    }

    public void setPending(long pending) {
        this.pending = pending;
    }

    public long getOverdue() {
        return overdue;
    }

    public void setOverdue(long overdue) {
        this.overdue = overdue;
    }

    public long getResolved() {
        return resolved;
    }

    public void setResolved(long resolved) {
        this.resolved = resolved;
    }
}

