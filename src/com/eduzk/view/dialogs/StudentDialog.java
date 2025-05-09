package com.eduzk.view.dialogs;

import com.eduzk.controller.StudentController;
import com.eduzk.model.entities.Student;
import com.eduzk.utils.UIUtils;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.view.components.CustomDatePicker;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class StudentDialog extends JDialog {

    private final StudentController controller;
    private final Student studentToEdit;
    private final boolean isEditMode;

    private JTextField idField;
    private JTextField nameField;
    private CustomDatePicker dobPicker;
    private JComboBox<String> genderComboBox;
    private JTextField addressField;
    private JTextField parentNameField;
    private JTextField phoneField;
    private JTextField emailField;
    private JButton saveButton;
    private JButton cancelButton;
    private JLabel passwordLabel;
    private JTextField passwordField;

    public StudentDialog(Frame owner, StudentController controller, Student student) {
        super(owner, true);
        this.controller = controller;
        this.studentToEdit = student;
        this.isEditMode = (student != null);

        setTitle(isEditMode ? "Edit Student Information" : "Add New Student");

        initComponents();
        setupLayout();
        setupActions();
        populateFields();
        configureDialog();
    }

    private void initComponents() {
        idField = new JTextField(5);
        idField.setEditable(false);
        idField.setToolTipText("Student ID (Auto-generated)");

        nameField = new JTextField(25);
        dobPicker = new CustomDatePicker();
        genderComboBox = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        addressField = new JTextField(30);
        parentNameField = new JTextField(25);
        phoneField = new JTextField(15);
        emailField = new JTextField(25);

        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");

        passwordLabel = new JLabel("Account Password:");
        passwordField = new JTextField(25);
        passwordField.setToolTipText("WARNING: Editing/Viewing plain text password. Consider using a 'Reset Password' feature instead.");
        passwordLabel.setVisible(false);
        passwordField.setVisible(false);
    }

    private void setupLayout() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        int currentRow = 0;

        if (isEditMode) {
            gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
            formPanel.add(new JLabel("Student ID:"), gbc);
            gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
            formPanel.add(idField, gbc);
            currentRow++;
        }

        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Full Name*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(nameField, gbc);
        currentRow++;

        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Date of Birth:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        formPanel.add(dobPicker, gbc);

        gbc.gridx = 2; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Gender:"), gbc);
        gbc.gridx = 3; gbc.gridy = currentRow; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        formPanel.add(genderComboBox, gbc);
        currentRow++;

        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(addressField, gbc);
        currentRow++;

        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Parent Name:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(parentNameField, gbc);
        currentRow++;

        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Phone (Username)*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(phoneField, gbc);
        currentRow++;

        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(emailField, gbc);
        currentRow++;

        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(passwordLabel, gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(passwordField, gbc);
        currentRow++;

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        setLayout(new BorderLayout(0, 10));
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupActions() {
        saveButton.addActionListener(e -> saveStudent());
        cancelButton.addActionListener(e -> dispose());
    }

    private void populateFields() {
        passwordLabel.setVisible(false);
        passwordField.setVisible(false);
        passwordField.setText("");

        if (isEditMode && studentToEdit != null) {
            idField.setText(String.valueOf(studentToEdit.getStudentId()));
            nameField.setText(studentToEdit.getFullName());
            dobPicker.setDate(studentToEdit.getDateOfBirth());
            genderComboBox.setSelectedItem(studentToEdit.getGender());
            addressField.setText(studentToEdit.getAddress());
            parentNameField.setText(studentToEdit.getParentName());
            phoneField.setText(studentToEdit.getPhone());
            emailField.setText(studentToEdit.getEmail());

            if (controller != null && controller.isCurrentUserAdmin()) {
                String currentPassword = controller.getPasswordForStudent(studentToEdit.getStudentId());
                if (currentPassword != null) {
                    passwordLabel.setVisible(true);
                    passwordField.setVisible(true);
                    passwordField.setText(currentPassword);
                } else {
                    passwordLabel.setVisible(true);
                    passwordField.setVisible(true);
                    passwordField.setText("");
                    passwordField.setToolTipText("Student has not registered an account yet.");
                    passwordLabel.setText("Account Password (N/A):");
                }
            }

        } else {
            genderComboBox.setSelectedIndex(0);
        }
        pack();
    }

    private void configureDialog() {
        setMinimumSize(new Dimension(450, 500));
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void saveStudent() {
        String name = nameField.getText().trim();
        LocalDate dob = dobPicker.getDate();
        String gender = (String) genderComboBox.getSelectedItem();
        String address = addressField.getText().trim();
        String parentName = parentNameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String newPassword = passwordField.getText();

        if (!ValidationUtils.isNotEmpty(name)) { UIUtils.showWarningMessage(this, "Validation Error", "Full Name cannot be empty."); nameField.requestFocusInWindow(); return; }
        if (!ValidationUtils.isNotEmpty(phone)) { UIUtils.showWarningMessage(this, "Validation Error", "Phone Number (Username) cannot be empty."); phoneField.requestFocusInWindow(); return; }
        if (!ValidationUtils.isValidPhoneNumber(phone)) { UIUtils.showWarningMessage(this, "Validation Error", "Invalid phone number format."); phoneField.requestFocusInWindow(); return; }
        if (ValidationUtils.isNotEmpty(email) && !ValidationUtils.isValidEmail(email)) { UIUtils.showWarningMessage(this, "Validation Error", "Invalid email address format."); emailField.requestFocusInWindow(); return; }

        Student student = isEditMode ? studentToEdit : new Student();
        student.setFullName(name);
        student.setDateOfBirth(dob);
        student.setGender(gender);
        student.setAddress(address);
        student.setParentName(parentName);
        student.setPhone(phone);
        student.setEmail(email);

        boolean studentSaveSuccess;
        if (isEditMode) {
            studentSaveSuccess = controller.updateStudent(student);
        } else {
            studentSaveSuccess = controller.addStudent(student);
        }

        if (isEditMode && studentSaveSuccess && controller.isCurrentUserAdmin() && passwordField.isVisible()) {
            String currentPasswordInDB = controller.getPasswordForStudent(studentToEdit.getStudentId());

            boolean shouldUpdatePassword = ValidationUtils.isNotEmpty(newPassword) &&
                    !newPassword.equals(currentPasswordInDB);

            if (shouldUpdatePassword) {
                if (!ValidationUtils.isValidPassword(newPassword)) {
                    UIUtils.showWarningMessage(this, "Password Error", "New password must be at least 6 characters long.");
                    passwordField.requestFocusInWindow();
                    return;
                } else {
                    System.out.println("Attempting to update password for student ID: " + studentToEdit.getStudentId());
                    boolean passUpdateSuccess = controller.updatePasswordForStudent(studentToEdit.getStudentId(), newPassword);
                }
            }
        }

        if (studentSaveSuccess) {
            dispose();
        }
    }
}