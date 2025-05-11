package com.eduzk.controller;

import com.eduzk.model.dao.interfaces.IUserDAO;
import com.eduzk.model.entities.LogEntry;
import com.eduzk.model.entities.Role;
import com.eduzk.model.entities.User;
import com.eduzk.model.exceptions.DataAccessException;
import com.eduzk.model.dao.impl.LogService;
import com.eduzk.utils.PasswordUtils;
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
    private final LogService logService;
    private AccountsPanel accountsPanel;

    public UserController(IUserDAO userDAO, User currentUser, LogService logService) {
        if (userDAO == null || currentUser == null || logService == null) {
            throw new IllegalArgumentException("DAO, CurrentUser, and LogService cannot be null in UserController");
        }
        this.userDAO = userDAO;
        this.currentUser = currentUser;
        this.logService = logService;
    }

    public void setAccountsPanel(AccountsPanel accountsPanel) {
        this.accountsPanel = accountsPanel;
    }

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

    public boolean updateUserPassword(int userId, String newPassword) {
        if (!isCurrentUserAdmin()) {
            UIUtils.showErrorMessage(accountsPanel, "Permission Denied", "Only administrators can change passwords.");
            return false;
        }
        if (userId <= 0) {
            UIUtils.showWarningMessage(accountsPanel, "Error", "Invalid User ID.");
            return false;
        }
        List<String> passwordErrors = ValidationUtils.getPasswordValidationErrors(newPassword);
        if (!passwordErrors.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Mật khẩu mới không hợp lệ cho người dùng:\n");
            for (String error : passwordErrors) {
                errorMessage.append(error.replace("- ", "  ")).append("\n");
            }
            UIUtils.showWarningMessage(accountsPanel, "Mật khẩu không hợp lệ", errorMessage.toString());
            return false;
        }

        User userToUpdate = null;
        try {
            userToUpdate = userDAO.getById(userId);
            if (userToUpdate == null) {
                UIUtils.showErrorMessage(accountsPanel, "Error", "User with ID " + userId + " not found.");
                return false;
            }
            String hashedPassword = PasswordUtils.hashPassword(newPassword);
            userToUpdate.setPassword(hashedPassword);
            userToUpdate.setRequiresPasswordChange(false);
            userDAO.update(userToUpdate);

            String logDetails = String.format("For User ID: %d, Username: %s",
                    userId, userToUpdate.getUsername());
            writeLog("Updated Password", logDetails);

            UIUtils.showInfoMessage(accountsPanel, "Success", "Password for user '" + userToUpdate.getUsername() + "' updated successfully.");
            if (accountsPanel != null) {
                accountsPanel.refreshTable();
            }
            return true;

        } catch (DataAccessException e) {
            System.err.println("Error updating password for user ID " + userId + ": " + e.getMessage());
            UIUtils.showErrorMessage(accountsPanel, "Error", "Failed to update password: " + e.getMessage());
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

    private boolean isCurrentUserAdmin() {
        return this.currentUser != null && this.currentUser.getRole() == Role.ADMIN;
    }

    private void writeLog(String action, String details) {
        if (logService != null && currentUser != null) {
            try {
                LogEntry log = new LogEntry(
                        LocalDateTime.now(),
                        currentUser.getDisplayName(),
                        currentUser.getRole().name(),
                        action,
                        details
                );
                logService.addLogEntry(log);
                System.out.println("Log written: " + log);
            } catch (Exception e) {
                System.err.println("!!! Failed to write log entry: Action=" + action + ", Details=" + details + " - Error: " + e.getMessage());
            }
        } else {
            System.err.println("LogService or CurrentUser is null. Cannot write log for action: " + action);
        }
    }

}