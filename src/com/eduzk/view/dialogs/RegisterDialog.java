package com.eduzk.view.dialogs;

import com.eduzk.controller.AuthController;
import com.eduzk.model.entities.Role;
import com.eduzk.utils.UIUtils;
import com.eduzk.utils.ValidationUtils;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class RegisterDialog extends JDialog {

    private final AuthController authController;

    private JTextField usernameOrPhoneField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JComboBox<Role> roleComboBox;
    private JLabel idLabel;
    private JTextField teacherIdField;
    private JButton registerButton;
    private JButton cancelButton;
    private JLabel statusLabel;

    public RegisterDialog(Frame owner, AuthController authController) {
        super(owner, "Register New Account", true);
        this.authController = authController;
        initComponents();
        setupLayout();
        setupActions();
        configureDialog();
        updateFieldsBasedOnRole();
    }

    private void initComponents() {
        usernameOrPhoneField = new JTextField(20);
        passwordField = new JPasswordField(20);
        confirmPasswordField = new JPasswordField(20);
        roleComboBox = new JComboBox<>(new Role[]{Role.STUDENT, Role.TEACHER});
        roleComboBox.setSelectedItem(Role.STUDENT);

        idLabel = new JLabel();
        teacherIdField = new JTextField(10);

        registerButton = new JButton("Register");
        cancelButton = new JButton("Cancel");
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void setupLayout() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        int currentRow = 0;

        // Username / Phone Number (row 0)
        gbc.gridx = 0; gbc.gridy = currentRow;
        formPanel.add(new JLabel("Username/Phone*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(usernameOrPhoneField, gbc);
        currentRow++;

        // Password (row 1)
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Password*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(passwordField, gbc);
        currentRow++;

        // Confirm Password (row 2)
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Confirm Password*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(confirmPasswordField, gbc);
        currentRow++;

        // Role Selection (row 3)
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Register as*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(roleComboBox, gbc);
        currentRow++;

        // Teacher ID (row 4)
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(idLabel, gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(teacherIdField, gbc);
        currentRow++;

        // Status Label (row 5)
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(statusLabel, gbc);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);

        // Add panels to dialog
        setLayout(new BorderLayout());
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupActions() {
        registerButton.addActionListener(e -> performRegistration());
        cancelButton.addActionListener(e -> dispose());
        roleComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateFieldsBasedOnRole();
            }
        });
    }

    private void updateFieldsBasedOnRole() {
        Role selectedRole = (Role) roleComboBox.getSelectedItem();
        boolean isTeacher = (selectedRole == Role.TEACHER);
        idLabel.setText(isTeacher ? "Your Teacher ID*:" : " ");
        teacherIdField.setVisible(isTeacher);
        idLabel.setVisible(isTeacher);
        usernameOrPhoneField.setToolTipText(isTeacher ? "Enter your desired username" : "Enter your phone number (must exist in student records)");


        this.pack();
    }

    private void configureDialog() {
        setResizable(false);
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void performRegistration() {
        String usernameOrPhone = usernameOrPhoneField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        Role selectedRole = (Role) roleComboBox.getSelectedItem();
        String teacherIdStr = teacherIdField.getText().trim();
        Integer teacherIdInput = null;

        statusLabel.setText(" ");

        // --- Validation Chung ---
        if (!ValidationUtils.isNotEmpty(usernameOrPhone) || !ValidationUtils.isNotEmpty(password)) { statusLabel.setText("Username/Phone and password required."); return; }
        if (!password.equals(confirmPassword)) { statusLabel.setText("Passwords do not match."); return; }
        if (!ValidationUtils.isValidPassword(password)) { statusLabel.setText("Password too short (min 6 chars)."); return; }

        // --- Validation Riêng theo Role ---
        if (selectedRole == Role.STUDENT) {
            if (!ValidationUtils.isValidPhoneNumber(usernameOrPhone)) {
                statusLabel.setText("Invalid phone number format for student registration.");
                usernameOrPhoneField.requestFocusInWindow();
                return;
            }
            teacherIdInput = null;
        } else if (selectedRole == Role.TEACHER) {
            if (!ValidationUtils.isValidUsername(usernameOrPhone)) {
                statusLabel.setText("Invalid username format for teacher (3-20 alphanumeric/underscore).");
                usernameOrPhoneField.requestFocusInWindow();
                return;
            }
            if (!ValidationUtils.isNotEmpty(teacherIdStr)) { statusLabel.setText("Teacher ID is required."); teacherIdField.requestFocusInWindow(); return; }
            try {
                teacherIdInput = Integer.parseInt(teacherIdStr);
                if (teacherIdInput <= 0) { statusLabel.setText("Invalid Teacher ID (must be positive)."); teacherIdField.requestFocusInWindow(); return; }
            } catch (NumberFormatException e) { statusLabel.setText("Invalid Teacher ID (must be a number)."); teacherIdField.requestFocusInWindow(); return; }
        } else {
            statusLabel.setText("Invalid role selected.");
            return;
        }

        registerButton.setEnabled(false);
        cancelButton.setEnabled(false);
        statusLabel.setText("Registering...");

        final Integer finalTeacherId = teacherIdInput;
        final String finalUsername = usernameOrPhone;
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return authController.attemptRegistration(finalUsername, password, confirmPassword, selectedRole, finalTeacherId);
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        UIUtils.showInfoMessage(RegisterDialog.this, "Registration Successful", "Account created successfully! You can now login.");
                        dispose();
                    } else {
                        passwordField.setText("");
                        confirmPasswordField.setText("");
                        usernameOrPhoneField.requestFocusInWindow();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    UIUtils.showErrorMessage(RegisterDialog.this, "Registration Error", "An unexpected error occurred.");
                    statusLabel.setText("Unexpected error.");
                } finally {
                    registerButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    if (!isDisplayable() || !isVisible()) { statusLabel.setText(" "); }
                    else if (statusLabel.getText().equals("Registering...")) { statusLabel.setText(" "); }
                }
            }
        };
        worker.execute();
    }
}
