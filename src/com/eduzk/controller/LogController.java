package com.eduzk.controller;

import com.eduzk.model.entities.LogEntry;
import com.eduzk.model.entities.Role;
import com.eduzk.model.entities.User;
import com.eduzk.model.service.LogService; // Import service
import com.eduzk.view.panels.LogsPanel;   // Import panel
import java.util.Collections;
import java.util.List;

public class LogController {

    private final LogService logService;
    private final User currentUser;
    private LogsPanel logsPanel;

    public LogController(LogService logService, User currentUser) {
        if (logService == null || currentUser == null) {
            throw new IllegalArgumentException("LogService and CurrentUser cannot be null");
        }
        this.logService = logService;
        this.currentUser = currentUser;
    }

    public void setLogsPanel(LogsPanel logsPanel) {
        this.logsPanel = logsPanel;
    }

    /**
     * Lấy danh sách log để hiển thị. Chỉ Admin mới có quyền.
     * @return Danh sách LogEntry hoặc rỗng.
     */
    public List<LogEntry> getAllLogsForDisplay() {
        if (currentUser.getRole() == Role.ADMIN) {
            return logService.getAllLogs(); // Lấy từ service
        } else {
            System.err.println("LogController: Access denied for user " + currentUser.getUsername());
            return Collections.emptyList(); // Trả về rỗng nếu không phải Admin
        }
    }
}