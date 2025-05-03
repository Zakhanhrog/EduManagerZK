package com.eduzk.controller;

import com.eduzk.model.dao.interfaces.IUserDAO;
import com.eduzk.model.entities.Role;
import com.eduzk.model.entities.User;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.utils.UIUtils;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.view.panels.AccountsPanel; // Sẽ tạo ở bước sau

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class UserController {

    private final IUserDAO userDAO;
    private final User currentUser; // Người dùng đang đăng nhập (để kiểm tra quyền)
    private AccountsPanel accountsPanel; // Tham chiếu đến view

    public UserController(IUserDAO userDAO, User currentUser) {
        if (userDAO == null || currentUser == null) {
            throw new IllegalArgumentException("DAO and CurrentUser cannot be null in UserController");
        }
        this.userDAO = userDAO;
        this.currentUser = currentUser;
    }

    public void setAccountsPanel(AccountsPanel accountsPanel) {
        this.accountsPanel = accountsPanel;
    }

    public List<User> getAllUserAccounts() {
        if (!isCurrentUserAdmin()) {
            // Chỉ Admin mới được xem tất cả tài khoản
            System.err.println("UserController: Permission denied for getAllUserAccounts by user " + currentUser.getUsername());
            return Collections.emptyList();
        }
        try {
            System.out.println("UserController.getAllUserAccounts: Calling userDAO.getAll()...");
            List<User> allUsers = userDAO.getAll();
            System.out.println("UserController.getAllUserAccounts: userDAO.getAll() returned " + (allUsers == null ? "null" : allUsers.size()) + " users.");
            if (allUsers == null) { return Collections.emptyList(); }
            List<User> filteredUsers = allUsers.stream()
                    .filter(user -> user.getRole() != Role.ADMIN)
                    .collect(Collectors.toList());

            System.out.println("UserController.getAllUserAccounts: Returning " + filteredUsers.size() + " non-admin users."); // Log số lượng sau lọc
            // In ra danh sách sau khi lọc (tùy chọn)
            return filteredUsers;

        } catch (DataAccessException e) {
            System.err.println("Error loading user accounts: " + e.getMessage());
            UIUtils.showErrorMessage(accountsPanel, "Error", "Failed to load user accounts.");
            return Collections.emptyList();
        }
    }
    public boolean updateUserPassword(int userId, String newPassword) {
        if (!isCurrentUserAdmin()) {
            UIUtils.showErrorMessage(accountsPanel, "Permission Denied", "Only administrators can change passwords.");
            return false;
        }
        if (userId <= 0) {
            UIUtils.showWarningMessage(accountsPanel, "Error", "Invalid User ID.");
            return false;
        }
        // Thêm validation cho newPassword nếu cần (ví dụ: không rỗng, độ dài tối thiểu)
        if (!ValidationUtils.isValidPassword(newPassword)) { // Dùng lại validation cũ
            // Tiếp tục trong file: src/com/eduzk/controller/UserController.java

            UIUtils.showWarningMessage(accountsPanel, "Validation Error", "Password must be at least 6 characters long.");
            return false;
        }

        try {
            User userToUpdate = userDAO.getById(userId);
            if (userToUpdate == null) {
                UIUtils.showErrorMessage(accountsPanel, "Error", "User with ID " + userId + " not found.");
                return false;
            }

            // Cập nhật mật khẩu
            userToUpdate.setPassword(newPassword); // Lưu mật khẩu mới (chưa hash)

            // Gọi DAO để lưu thay đổi
            userDAO.update(userToUpdate);

            // Thông báo thành công và refresh (nếu cần)
            UIUtils.showInfoMessage(accountsPanel, "Success", "Password for user '" + userToUpdate.getUsername() + "' updated successfully.");
            if (accountsPanel != null) {
                accountsPanel.refreshTable(); // Yêu cầu panel cập nhật lại bảng
            }
            return true;

        } catch (DataAccessException e) {
            System.err.println("Error updating password for user ID " + userId + ": " + e.getMessage());
            UIUtils.showErrorMessage(accountsPanel, "Error", "Failed to update password: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected error updating password for user ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
            UIUtils.showErrorMessage(accountsPanel, "Unexpected Error", "An unexpected error occurred while updating the password.");
            return false;
        }
    }

    private boolean isCurrentUserAdmin() {
        return this.currentUser != null && this.currentUser.getRole() == Role.ADMIN;
    }

}