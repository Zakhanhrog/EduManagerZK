package com.eduzk.view.dialogs;

import com.eduzk.controller.CourseController;
import com.eduzk.model.entities.Course;
import com.eduzk.utils.UIUtils;
import com.eduzk.utils.ValidationUtils;

import javax.swing.*;
import java.awt.*;

public class CourseDialog extends JDialog {

    private final CourseController controller;
    private final Course courseToEdit;
    private final boolean isEditMode;

    private JTextField idField;
    private JTextField codeField;
    private JTextField nameField;
    private JTextField levelField;
    private JSpinner creditsSpinner;
    private JTextArea descriptionArea;
    private JButton saveButton;
    private JButton cancelButton;

    public CourseDialog(Frame owner, CourseController controller, Course course) {
        super(owner, true);
        this.controller = controller;
        this.courseToEdit = course;
        this.isEditMode = (course != null);

        setTitle(isEditMode ? "Edit Course" : "Add Course");
        initComponents();
        setupLayout();
        setupActions();
        populateFields();
        configureDialog();
    }

    private void initComponents() {
        idField = new JTextField(5);
        idField.setEditable(false);
        codeField = new JTextField(10);
        nameField = new JTextField(25);
        levelField = new JTextField(15);
        creditsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        descriptionArea = new JTextArea(4, 25); // Rows, Columns
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

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

        // Row: ID (Edit mode only)
        if (isEditMode) {
            gbc.gridx = 0; gbc.gridy = currentRow;
            formPanel.add(new JLabel("ID:"), gbc);
            gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
            formPanel.add(idField, gbc);
            currentRow++;
        }

        // Row: Code & Name
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Code*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.3;
        formPanel.add(codeField, gbc);

        gbc.gridx = 2; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Name*:"), gbc);
        gbc.gridx = 3; gbc.gridy = currentRow; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.7;
        formPanel.add(nameField, gbc);
        currentRow++;

        // Row: Level & Credits
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("Level:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.3;
        formPanel.add(levelField, gbc);

        gbc.gridx = 2; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Credits:"), gbc);
        gbc.gridx = 3; gbc.gridy = currentRow; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.7;
        formPanel.add(creditsSpinner, gbc);
        currentRow++;


        // Row: Description
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.NORTHWEST; // Align label top-left
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0; // Allow area to grow
        formPanel.add(new JScrollPane(descriptionArea), gbc);
        currentRow++;


        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        // Add panels to dialog
        setLayout(new BorderLayout());
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupActions() {
        saveButton.addActionListener(e -> saveCourse());
        cancelButton.addActionListener(e -> dispose());
    }

    private void populateFields() {
        if (isEditMode && courseToEdit != null) {
            idField.setText(String.valueOf(courseToEdit.getCourseId()));
            codeField.setText(courseToEdit.getCourseCode());
            nameField.setText(courseToEdit.getCourseName());
            levelField.setText(courseToEdit.getLevel());
            creditsSpinner.setValue(courseToEdit.getCredits());
            descriptionArea.setText(courseToEdit.getDescription());
        } else {
            creditsSpinner.setValue(0);
        }
    }

    private void configureDialog() {
        pack();
        setMinimumSize(new Dimension(450, 300));
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void saveCourse() {
        // --- Input Validation ---
        String code = codeField.getText().trim();
        String name = nameField.getText().trim();
        String level = levelField.getText().trim();
        int credits = (int) creditsSpinner.getValue();
        String description = descriptionArea.getText().trim();
        if (!ValidationUtils.isNotEmpty(code)) {
            UIUtils.showWarningMessage(this, "Validation Error", "Course Code cannot be empty.");
            codeField.requestFocusInWindow();
            return;
        }
        if (!ValidationUtils.isNotEmpty(name)) {
            UIUtils.showWarningMessage(this, "Validation Error", "Course Name cannot be empty.");
            nameField.requestFocusInWindow();
            return;
        }
        if (credits < 0) {
            UIUtils.showWarningMessage(this, "Validation Error", "Credits cannot be negative.");
            creditsSpinner.requestFocusInWindow();
            return;
        }

        Course course = isEditMode ? courseToEdit : new Course();
        course.setCourseCode(code);
        course.setCourseName(name);
        course.setLevel(level);
        course.setCredits(credits);
        course.setDescription(description);
        boolean success;
        if (isEditMode) {
            success = controller.updateCourse(course);
        } else {
            success = controller.addCourse(course);
        }

        if (success) {
            dispose();
        }
    }
}