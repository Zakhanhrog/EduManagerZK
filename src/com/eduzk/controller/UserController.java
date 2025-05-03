package com.eduzk.controller;

import com.eduzk.model.dao.interfaces.IUserDAO;
import com.eduzk.model.entities.LogEntry;
import com.eduzk.model.entities.Role;
import com.eduzk.model.entities.User;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.model.service.LogService;
import com.eduzk.utils.UIUtils;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.view.panels.AccountsPanel;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class UserController {

    private final IUserDAO userDAO;
    private final User currentUser;
    private final LogService logService; // <-- THÊM BIẾN LOGSERVICE
    private AccountsPanel accountsPanel;

    // --- SỬA CONSTRUCTOR ĐỂ NHẬN LogService ---
    public UserController(IUserDAO userDAO, User currentUser, LogService logService) { // Thêm LogService
        if (userDAO == null || currentUser == null || logService == null) { // Kiểm tra cả logService
            throw new IllegalArgumentException("DAO, CurrentUser, and LogService cannot be null in UserController");
        }
        this.userDAO = userDAO;
        this.currentUser = currentUser;
        this.logService = logService; // <-- LƯU LogService
    }

    public void setAccountsPanel(AccountsPanel accountsPanel) {
        this.accountsPanel = accountsPanel;
    }

    // --- getAllUserAccounts (Giữ nguyên logic lọc Admin) ---
    public List<User> getAllUserAccounts() {
        if (!isCurrentUserAdmin()) {
            System.err.println("UserController: Permission denied for getAllUserAccounts by user " + currentUser.getUsername());
            return Collections.emptyList();
        }
        try {
            System.out.println("UserController.getAllUserAccounts: Calling userDAO.getAll()...");
            List<User> allUsers = userDAO.getAll();
            System.out.println("UserController.getAllUserAccounts: userDAO.getAll() returned " + (allUsers == null ? "null" : allUsers.size()) + " users.");
            if (allUsers == null) { return Collections.emptyList(); }

            // Lọc bỏ Admin ngay tại đây
            List<User> filteredUsers = allUsers.stream()
                    .filter(user -> user.getRole() != Role.ADMIN)
                    .collect(Collectors.toList());

            System.out.println("UserController.getAllUserAccounts: Returning " + filteredUsers.size() + " non-admin users.");
            return filteredUsers;

        } catch (DataAccessException e) {
            System.err.println("Error loading user accounts: " + e.getMessage());
            UIUtils.showErrorMessage(accountsPanel, "Error", "Failed to load user accounts.");
            return Collections.emptyList();
        }
    }

    // --- updateUserPassword (Đã thêm ghi log) ---
    public boolean updateUserPassword(int userId, String newPassword) {
        // --- Kiểm tra quyền ---
        if (!isCurrentUserAdmin()) {
            UIUtils.showErrorMessage(accountsPanel, "Permission Denied", "Only administrators can change passwords.");
            return false;
        }
        // --- Validation đầu vào ---
        if (userId <= 0) {
            UIUtils.showWarningMessage(accountsPanel, "Error", "Invalid User ID.");
            return false;
        }
        if (!ValidationUtils.isValidPassword(newPassword)) {
            UIUtils.showWarningMessage(accountsPanel, "Validation Error", "Password must be at least 6 characters long.");
            return false;
        }

        User userToUpdate = null; // Khai báo ngoài try để dùng trong log lỗi
        try {
            userToUpdate = userDAO.getById(userId);
            if (userToUpdate == null) {
                UIUtils.showErrorMessage(accountsPanel, "Error", "User with ID " + userId + " not found.");
                return false;
            }

            // Lưu mật khẩu cũ (nếu muốn ghi log chi tiết hơn về thay đổi)
            // String oldPassword = userToUpdate.getPassword();

            // Cập nhật mật khẩu mới
            userToUpdate.setPassword(newPassword);

            // Gọi DAO để lưu
            userDAO.update(userToUpdate);

            // *** GHI LOG SAU KHI THÀNH CÔNG ***
            // Tạo chi tiết log
            String logDetails = String.format("For User ID: %d, Username: %s",
                    userId, userToUpdate.getUsername());
            // Có thể thêm: ", From old password (masked): ***"
            writeLog("Updated Password", logDetails);

            // Thông báo thành công và refresh
            UIUtils.showInfoMessage(accountsPanel, "Success", "Password for user '" + userToUpdate.getUsername() + "' updated successfully.");
            if (accountsPanel != null) {
                accountsPanel.refreshTable();
            }
            return true;

        } catch (DataAccessException e) {
            System.err.println("Error updating password for user ID " + userId + ": " + e.getMessage());
            UIUtils.showErrorMessage(accountsPanel, "Error", "Failed to update password: " + e.getMessage());
            // Ghi log lỗi nếu muốn (có thể ghi lại thông tin user nếu lấy được)
            String errorLogDetails = "For User ID: " + userId + (userToUpdate != null ? ", Username: " + userToUpdate.getUsername() : "") + " - Error: " + e.getMessage();
            writeLog("Update Password Failed (DAO)", errorLogDetails);
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected error updating password for user ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
            UIUtils.showErrorMessage(accountsPanel, "Unexpected Error", "An unexpected error occurred while updating the password.");
            String errorLogDetails = "For User ID: " + userId + (userToUpdate != null ? ", Username: " + userToUpdate.getUsername() : "") + " - Unexpected Error: " + e.getMessage();
            writeLog("Update Password Failed (Unexpected)", errorLogDetails);
            return false;
        }
    }

    // --- isCurrentUserAdmin (Giữ nguyên) ---
    private boolean isCurrentUserAdmin() {
        return this.currentUser != null && this.currentUser.getRole() == Role.ADMIN;
    }

    // --- HÀM HELPER GHI LOG CHUNG (Copy từ Controller khác) ---
    private void writeLog(String action, String details) {
        if (logService != null && currentUser != null) {
            try {
                LogEntry log = new LogEntry(
                        LocalDateTime.now(),
                        currentUser.getDisplayName(), // Sử dụng DisplayName của người thực hiện
                        currentUser.getRole().name(),
                        action,
                        details
                );
                logService.addLogEntry(log);
                System.out.println("Log written: " + log); // Log ra console để dễ theo dõi
            } catch (Exception e) {
                System.err.println("!!! Failed to write log entry: Action=" + action + ", Details=" + details + " - Error: " + e.getMessage());
                // Không nên để lỗi ghi log làm ảnh hưởng luồng chính
            }
        } else {
            System.err.println("LogService or CurrentUser is null. Cannot write log for action: " + action);
        }
    }

    // Có thể thêm các hàm helper ghi log cụ thể nếu cần
    // Ví dụ:
    // private void writeUpdatePasswordLog(User updatedUser) {
    //     String details = "For User ID: " + updatedUser.getUserId() + ", Username: " + updatedUser.getUsername();
    //     writeLog("Updated Password", details);
    // }

} // Kết thúc lớp UserController