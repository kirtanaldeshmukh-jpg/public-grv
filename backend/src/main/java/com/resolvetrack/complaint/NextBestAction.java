package com.resolvetrack.complaint;

import java.util.Collections;
import java.util.Map;

public class NextBestAction {
    private String action;
    private String title;
    private String reason;
    private String targetRole;
    private String urgency;
    private String actionKey;
    private String reasonKey;
    private Map<String, Object> params;

    public NextBestAction() {
        this.params = Collections.emptyMap();
    }

    public NextBestAction(
            String action,
            String title,
            String reason,
            String targetRole,
            String urgency,
            String actionKey,
            String reasonKey,
            Map<String, Object> params) {
        this.action = action;
        this.title = title;
        this.reason = reason;
        this.targetRole = targetRole;
        this.urgency = urgency;
        this.actionKey = actionKey != null ? actionKey : "nba.action." + action;
        this.reasonKey = reasonKey != null ? reasonKey : "nba.reason.default";
        this.params = params != null ? params : Collections.emptyMap();
    }

    public NextBestAction(String action, String title, String reason, String targetRole, String urgency) {
        this(action, title, reason, targetRole, urgency, "nba.action." + action, "nba.reason.default", Collections.emptyMap());
    }

    public static NextBestAction of(String action, String title, String reason, String targetRole, String urgency) {
        return new NextBestAction(action, title, reason, targetRole, urgency);
    }

    public static NextBestAction of(
            String action,
            String title,
            String reason,
            String targetRole,
            String urgency,
            String actionKey,
            String reasonKey,
            Map<String, Object> params) {
        return new NextBestAction(action, title, reason, targetRole, urgency, actionKey, reasonKey, params);
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public String getActionKey() { return actionKey; }
    public void setActionKey(String actionKey) { this.actionKey = actionKey; }
    public String getReasonKey() { return reasonKey; }
    public void setReasonKey(String reasonKey) { this.reasonKey = reasonKey; }
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }

    @Override
    public String toString() {
        return "NextBestAction{" +
                "action='" + action + '\'' +
                ", title='" + title + '\'' +
                ", reason='" + reason + '\'' +
                ", targetRole='" + targetRole + '\'' +
                ", urgency='" + urgency + '\'' +
                ", actionKey='" + actionKey + '\'' +
                ", reasonKey='" + reasonKey + '\'' +
                '}';
    }
}
