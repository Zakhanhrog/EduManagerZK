package com.eduzk.view.dialogs;

import com.eduzk.controller.TeacherController;
import com.eduzk.model.entities.Teacher;
import com.eduzk.utils.UIUtils;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.view.components.CustomDatePicker;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class TeacherDialog extends JDialog {

    private final TeacherController controller;
    private final Teacher teacherToEdit;
    private final boolean isEditMode;

    private JTextField idField;
    private JTextField nameField;
    private CustomDatePicker dobPicker;
    private JComboBox<String> genderComboBox;
    private JTextField specializationField;
    private JTextField phoneField;
    private JTextField emailField;
    private JCheckBox activeCheckBox;
    private JButton saveButton;
    private JButton cancelButton;

    public TeacherDialog(Frame owner, TeacherController controller, Teacher teacher) {
        super(owner, true);
        this.controller = controller;
        this.teacherToEdit = teacher;
        this.isEditMode = (teacher != null);

        setTitle(isEditMode ? "Edit Teacher" : "Add Teacher");
        initComponents();
        setupLayout();
        setupActions();
        populateFields();
        configureDialog();
    }

    private void initComponents() {
        idField = new JTextField(5);
        idField.setEditable(false);
        nameField = new JTextField(25);
        dobPicker = new CustomDatePicker();
        genderComboBox = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        specializationField = new JTextField(25);
        phoneField = new JTextField(15);
        emailField = new JTextField(25);
        activeCheckBox = new JCheckBox("Active");
        activeCheckBox.setSelected(true);

        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
    }

    private void setupLayout() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        int currentRow = 0;

        if (isEditMode) {
            gbc.gridx = 0; gbc.gridy = currentRow;
            formPanel.add(new JLabel("ID:"), gbc);
            gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
            formPanel.add(idField, gbc);
            currentRow++;
        }

        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Full Name*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(nameField, gbc);
        currentRow++;

        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Date of Birth:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        formPanel.add(dobPicker, gbc);

        gbc.gridx = 2; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Gender:"), gbc);
        gbc.gridx = 3; gbc.gridy = currentRow; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        formPanel.add(genderComboBox, gbc);
        currentRow++;


        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Specialization:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(specializationField, gbc);
        currentRow++;

        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        formPanel.add(phoneField, gbc);

        gbc.gridx = 2; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 3; gbc.gridy = currentRow; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        formPanel.add(emailField, gbc);
        currentRow++;


        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(activeCheckBox, gbc);
        currentRow++;


        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        setLayout(new BorderLayout());
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupActions() {
        saveButton.addActionListener(e -> saveTeacher());
        cancelButton.addActionListener(e -> dispose());
    }

    private void populateFields() {
        if (isEditMode && teacherToEdit != null) {
            idField.setText(String.valueOf(teacherToEdit.getTeacherId()));
            nameField.setText(teacherToEdit.getFullName());
            dobPicker.setDate(teacherToEdit.getDateOfBirth());
            genderComboBox.setSelectedItem(teacherToEdit.getGender());
            specializationField.setText(teacherToEdit.getSpecialization());
            phoneField.setText(teacherToEdit.getPhone());
            emailField.setText(teacherToEdit.getEmail());
            activeCheckBox.setSelected(teacherToEdit.isActive());
        } else {
            genderComboBox.setSelectedIndex(0);
            activeCheckBox.setSelected(true);
        }
    }

    private void configureDialog() {
        pack();
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void saveTeacher() {
        String name = nameField.getText().trim();
        LocalDate dob = dobPicker.getDate();
        String gender = (String) genderComboBox.getSelectedItem();
        String specialization = specializationField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        boolean isActive = activeCheckBox.isSelected();

        if (!ValidationUtils.isNotEmpty(name)) {
            UIUtils.showWarningMessage(this, "Validation Error", "Full Name cannot be empty.");
            nameField.requestFocusInWindow();
            return;
        }
        if (ValidationUtils.isNotEmpty(phone) && !ValidationUtils.isValidPhoneNumber(phone)) {
            UIUtils.showWarningMessage(this, "Validation Error", "Invalid phone number format.");
            phoneField.requestFocusInWindow();
            return;
        }
        if (ValidationUtils.isNotEmpty(email) && !ValidationUtils.isValidEmail(email)) {
            UIUtils.showWarningMessage(this, "Validation Error", "Invalid email address format.");
            emailField.requestFocusInWindow();
            return;
        }

        Teacher teacher = isEditMode ? teacherToEdit : new Teacher();
        teacher.setFullName(name);
        teacher.setDateOfBirth(dob);
        teacher.setGender(gender);
        teacher.setSpecialization(specialization);
        teacher.setPhone(phone);
        teacher.setEmail(email);
        teacher.setActive(isActive);

        boolean success;
        if (isEditMode) {
            success = controller.updateTeacher(teacher);
        } else {
            success = controller.addTeacher(teacher);
        }

        if (success) {
            dispose();
        }
    }
}