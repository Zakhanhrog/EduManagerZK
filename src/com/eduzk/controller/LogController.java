package com.eduzk.controller;

import com.eduzk.model.dao.interfaces.LogEventListener;
import com.eduzk.model.entities.LogEntry;
import com.eduzk.model.entities.Role;
import com.eduzk.model.entities.User;
import com.eduzk.model.dao.impl.LogService;
import com.eduzk.view.panels.LogsPanel;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities; // <<< IMPORT SWING UTILITIES

// <<< IMPLEMENT LogEventListener >>>
public class LogController implements LogEventListener {

    private final LogService logService;
    private final User currentUser;
    private LogsPanel logsPanel; // Giữ tham chiếu đến panel

    public LogController(LogService logService, User currentUser) {
        if (logService == null || currentUser == null) {
            throw new IllegalArgumentException("LogService and CurrentUser cannot be null");
        }
        this.logService = logService;
        this.currentUser = currentUser;

        // <<< ĐĂNG KÝ LISTENER VỚI SERVICE >>>
        this.logService.addLogEventListener(this);
    }

    public void setLogsPanel(LogsPanel logsPanel) {
        this.logsPanel = logsPanel;
        // <<< CÓ THỂ GỌI TẢI DỮ LIỆU BAN ĐẦU Ở ĐÂY KHI PANEL ĐƯỢC SET >>>
        if (this.logsPanel != null && this.currentUser.getRole() == Role.ADMIN) {
            System.out.println("LogController: LogsPanel set, requesting initial data load.");
            requestPanelRefresh();
        }
    }

    /**
     * Lấy danh sách log để hiển thị. Chỉ Admin mới có quyền.
     * Được gọi bởi LogsPanel khi cần refresh thủ công hoặc ban đầu.
     * @return Danh sách LogEntry hoặc rỗng.
     */
    public List<LogEntry> getAllLogsForDisplay() {
        if (currentUser.getRole() == Role.ADMIN) {
            System.out.println("LogController: Admin requested all logs for display.");
            return logService.getAllLogs(); // Lấy từ service
        } else {
            System.err.println("LogController: Access denied for user " + currentUser.getUsername() + " to get logs.");
            return Collections.emptyList();
        }
    }

    // <<< PHƯƠNG THỨC MỚI ĐỂ YÊU CẦU PANEL CẬP NHẬT TỪ LISTENER >>>
    public void requestPanelRefresh() {
        if (logsPanel != null) {
            System.out.println("LogController: Requesting LogsPanel refresh on EDT.");
            // Đảm bảo cập nhật UI trên Event Dispatch Thread
            SwingUtilities.invokeLater(() -> {
                // Gọi phương thức refresh của LogsPanel
                logsPanel.refreshTable();
            });
        } else {
            System.err.println("LogController: Cannot request panel refresh, LogsPanel is null.");
        }
    }


    // <<< IMPLEMENT PHƯƠNG THỨC TỪ LogEventListener >>>
    @Override
    public void logAdded(LogEntry newLogEntry) {
        System.out.println("LogController: Received logAdded notification.");
        // Khi có log mới, yêu cầu panel cập nhật
        // Chỉ cập nhật nếu người dùng hiện tại là Admin (vì chỉ Admin thấy panel này)
        if (currentUser.getRole() == Role.ADMIN) {
            requestPanelRefresh();
        } else {
            // Nếu không phải admin thì không làm gì cả
            System.out.println("LogController: Received logAdded notification, but current user is not Admin. No UI update needed.");
        }
    }

    // <<< PHƯƠNG THỨC DỌN DẸP LISTENER >>>
    public void cleanupListener() {
        if (this.logService != null) {
            this.logService.removeLogEventListener(this);
            System.out.println("LogController: Unregistered listener from LogService.");
        }
    }
}