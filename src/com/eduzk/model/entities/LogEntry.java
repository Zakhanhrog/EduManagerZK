package com.eduzk.model.entities;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class LogEntry implements Serializable {
    private static final long serialVersionUID = 3L;

    private LocalDateTime timestamp;
    private String username;
    private String userRole;
    private String action;
    private String details;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LogEntry(LocalDateTime timestamp, String username, String userRole, String action, String details) {
        this.timestamp = timestamp;
        this.username = username != null ? username : "System";
        this.userRole = userRole != null ? userRole : "N/A";
        this.action = action != null ? action : "Unknown Action";
        this.details = details != null ? details : "";
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public String getUsername() { return username; }
    public String getUserRole() { return userRole; }
    public String getAction() { return action; }
    public String getDetails() { return details; }

    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(formatter) : "N/A";
    }

    @Override
    public String toString() {
        return getFormattedTimestamp() + " | " + username + " (" + userRole + ") | " + action + " | " + details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEntry logEntry = (LogEntry) o;
        return Objects.equals(timestamp, logEntry.timestamp) &&
                Objects.equals(username, logEntry.username) &&
                Objects.equals(action, logEntry.action) &&
                Objects.equals(details, logEntry.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, username, action, details);
    }
}