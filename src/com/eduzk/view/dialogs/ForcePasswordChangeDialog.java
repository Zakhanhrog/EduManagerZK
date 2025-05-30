package com.eduzk.view.dialogs;

import com.eduzk.controller.AuthController;
import com.eduzk.model.entities.User;
import com.eduzk.utils.UIUtils;
import com.eduzk.utils.ValidationUtils;
import com.formdev.flatlaf.FlatClientProperties;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class ForcePasswordChangeDialog extends JDialog {

    private final AuthController authController;
    private final User userToUpdate;

    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private JButton changeButton;
    private JButton cancelButton;
    private JLabel messageLabel;
    private JLabel errorLabel;

    public ForcePasswordChangeDialog(Frame owner, AuthController authController, User userToUpdate) {
        super(owner, "Change Initial Password", true);

        if (authController == null) {
            throw new IllegalArgumentException("AuthController cannot be null.");
        }
        if (userToUpdate == null) {
            throw new IllegalArgumentException("User object cannot be null.");
        }
        this.authController = authController;
        this.userToUpdate = userToUpdate;

        initComponents();
        setupLayout();
        setupActions();
        configureDialog();
    }

    private void initComponents() {
        messageLabel = new JLabel(
                "<html><center>Welcome, <b>" + userToUpdate.getDisplayName() + "</b>!<br>" +
                        "For security, please set a new password to continue.</center></html>",
                SwingConstants.CENTER
        );
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.BOLD, 13f));
        // Password fields
        newPasswordField = new JPasswordField(25);
        confirmPasswordField = new JPasswordField(25);

        // Buttons
        changeButton = new JButton("Change Password");
        cancelButton = new JButton("Cancel");

        // Error label (initially empty)
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(UIManager.getColor("Label.errorForeground"));
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        errorLabel.setFont(errorLabel.getFont().deriveFont(Font.ITALIC));
        newPasswordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Mật khẩu mới (ít nhất 8 ký tự, hoa, thường, số, đặc biệt)");
        confirmPasswordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Confirm new password");
        newPasswordField.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
        confirmPasswordField.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
        changeButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, "primary");
    }

    private void setupLayout() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 15, 5);

        // Add Message Label
        mainPanel.add(messageLabel, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1; // Reset to one column
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(10, 5, 5, 5);
        mainPanel.add(new JLabel("New Password:"), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(newPasswordField, gbc);

        // --- Confirm Password Row ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Confirm Password:"), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(confirmPasswordField, gbc);

        // --- Error Label Row ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 10, 5);
        mainPanel.add(errorLabel, gbc);

        // --- Button Row ---
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(10, 5, 0, 5);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.add(cancelButton);
        buttonPanel.add(changeButton);
        mainPanel.add(buttonPanel, gbc);

        this.setContentPane(mainPanel);
    }
    private void setupActions() {
        changeButton.addActionListener(e -> performPasswordChange());
        confirmPasswordField.addActionListener(e -> performPasswordChange());
        cancelButton.addActionListener(e -> dispose());
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("ForcePasswordChangeDialog: Window closed by user (treated as Cancel).");
                dispose();
            }
        });
    }

    private void performPasswordChange() {
        errorLabel.setText(" ");
        String newPassword = new String(newPasswordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        if (!ValidationUtils.isNotEmpty(newPassword) || !ValidationUtils.isNotEmpty(confirmPassword)) {
            errorLabel.setText("Please enter and confirm the new password.");
             UIUtils.showWarningMessage(this, "Input Required", "Please enter and confirm the new password."); // Alternative
            return;
        }
        List<String> passwordErrors = ValidationUtils.getPasswordValidationErrors(newPassword);
        if (!passwordErrors.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("<html>Mật khẩu không hợp lệ:<br>");
            for (String error : passwordErrors) {
                errorMessage.append(error).append("<br>");
            }
            errorMessage.append("</html>");

            errorLabel.setText(errorMessage.toString());
            UIUtils.showWarningMessage(this, "Mật khẩu không hợp lệ", errorMessage.toString().replace("<br>", "\n").replace("<html>", "").replace("</html>", ""));
            newPasswordField.requestFocusInWindow();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            errorLabel.setText("The entered passwords do not match.");
            confirmPasswordField.setText("");
            confirmPasswordField.requestFocusInWindow();
            return;
        }
        boolean success = authController.performForcedPasswordChange(userToUpdate, newPassword);

        if (success) {
             UIUtils.showInfoMessage(this, "Success", "Password changed successfully.");
            System.out.println("ForcePasswordChangeDialog: Password change successful. Closing dialog.");
            dispose();
        } else {
            errorLabel.setText("Failed to update password. Please try again.");
             UIUtils.showErrorMessage(this, "Error", "Failed to update password. Please try again or contact support.");
        }
    }

    private void configureDialog() {
        pack();
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }
}