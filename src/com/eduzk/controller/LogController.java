package com.eduzk.controller;

import com.eduzk.model.dao.interfaces.LogEventListener;
import com.eduzk.model.entities.LogEntry;
import com.eduzk.model.entities.Role;
import com.eduzk.model.entities.User;
import com.eduzk.model.dao.impl.LogService;
import com.eduzk.view.panels.LogsPanel;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;

public class LogController implements LogEventListener {

    private final LogService logService;
    private final User currentUser;
    private LogsPanel logsPanel;

    public LogController(LogService logService, User currentUser) {
        if (logService == null || currentUser == null) {
            throw new IllegalArgumentException("LogService and CurrentUser cannot be null");
        }
        this.logService = logService;
        this.currentUser = currentUser;

        this.logService.addLogEventListener(this);
    }

    public void setLogsPanel(LogsPanel logsPanel) {
        this.logsPanel = logsPanel;
        if (this.logsPanel != null && this.currentUser.getRole() == Role.ADMIN) {
            System.out.println("LogController: LogsPanel set, requesting initial data load.");
            requestPanelRefresh();
        }
    }
    public List<LogEntry> getAllLogsForDisplay() {
        if (currentUser.getRole() == Role.ADMIN) {
            System.out.println("LogController: Admin requested all logs for display.");
            return logService.getAllLogs();
        } else {
            System.err.println("LogController: Access denied for user " + currentUser.getUsername() + " to get logs.");
            return Collections.emptyList();
        }
    }

    public void requestPanelRefresh() {
        if (logsPanel != null) {
            System.out.println("LogController: Requesting LogsPanel refresh on EDT.");
            SwingUtilities.invokeLater(() -> {
                logsPanel.refreshTable();
            });
        } else {
            System.err.println("LogController: Cannot request panel refresh, LogsPanel is null.");
        }
    }

    @Override
    public void logAdded(LogEntry newLogEntry) {
        System.out.println("LogController: Received logAdded notification.");
        if (currentUser.getRole() == Role.ADMIN) {
            requestPanelRefresh();
        } else {
            System.out.println("LogController: Received logAdded notification, but current user is not Admin. No UI update needed.");
        }
    }

    public void cleanupListener() {
        if (this.logService != null) {
            this.logService.removeLogEventListener(this);
            System.out.println("LogController: Unregistered listener from LogService.");
        }
    }
}