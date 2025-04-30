package com.eduzk.view.dialogs;

import com.eduzk.controller.EduClassController;
import com.eduzk.model.entities.Course;
import com.eduzk.model.entities.EduClass;
import com.eduzk.model.entities.Teacher;
import com.eduzk.utils.UIUtils;
import com.eduzk.utils.ValidationUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

public class ClassDialog extends JDialog {

    private final EduClassController controller;
    private final EduClass classToEdit;
    private final boolean isEditMode;
    private final List<Course> availableCourses;
    private final List<Teacher> availableTeachers;

    // UI Components
    private JTextField idField;
    private JTextField classNameField;
    private JComboBox<Course> courseComboBox;
    private JComboBox<Teacher> teacherComboBox;
    private JTextField yearField;
    private JTextField semesterField;
    private JSpinner capacitySpinner;
    private JButton saveButton;
    private JButton cancelButton;

    public ClassDialog(Frame owner, EduClassController controller, EduClass eduClass,
                       List<Course> courses, List<Teacher> teachers) {
        super(owner, true);
        this.controller = controller;
        this.classToEdit = eduClass;
        this.isEditMode = (eduClass != null);
        this.availableCourses = courses;
        this.availableTeachers = teachers;

        setTitle(isEditMode ? "Edit Class" : "Add Class");
        initComponents();
        setupLayout();
        setupActions();
        populateFields();
        configureDialog();
    }

    private void initComponents() {
        idField = new JTextField(5);
        idField.setEditable(false);
        classNameField = new JTextField(25);
        courseComboBox = new JComboBox<>(new Vector<>(availableCourses)); // Use Vector for JComboBox model
        teacherComboBox = new JComboBox<>(new Vector<>(availableTeachers));
        yearField = new JTextField(10); // E.g., 2024-2025
        semesterField = new JTextField(10); // E.g., Fall, Spring, 1
        capacitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 500, 1)); // Min capacity 1

        // Custom renderers to display course/teacher names nicely in ComboBox
        courseComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Course) {
                    Course c = (Course) value;
                    setText(c.getCourseCode() + " - " + c.getCourseName());
                } else if (value == null && index == -1) {
                    // Handle display when nothing is selected (if needed)
                    setText(""); // Or "Select Course"
                }
                return this;
            }
        });

        teacherComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Teacher) {
                    Teacher t = (Teacher) value;
                    setText(t.getFullName());
                } else if (value == null && index == -1) {
                    setText(""); // Or "Select Teacher"
                }
                return this;
            }
        });


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
            gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
            formPanel.add(idField, gbc);
            currentRow++;
        }

        // Row: Class Name
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Class Name*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(classNameField, gbc);
        currentRow++;

        // Row: Course
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Course*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(courseComboBox, gbc);
        currentRow++;

        // Row: Teacher
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Primary Teacher*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(teacherComboBox, gbc);
        currentRow++;

        // Row: Year & Semester
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Academic Year:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        formPanel.add(yearField, gbc);

        gbc.gridx = 2; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Semester:"), gbc);
        gbc.gridx = 3; gbc.gridy = currentRow; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        formPanel.add(semesterField, gbc);
        currentRow++;

        // Row: Capacity
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("Max Capacity*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.0; // Don't let spinner stretch too much
        formPanel.add(capacitySpinner, gbc);
        // Add empty components to fill remaining space in this row if needed
        // gbc.gridx = 2; gbc.gridy = currentRow; gbc.weightx = 1.0; formPanel.add(new JLabel(""), gbc);


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
        saveButton.addActionListener(e -> saveClass());
        cancelButton.addActionListener(e -> dispose());
    }

    private void populateFields() {
        if (isEditMode && classToEdit != null) {
            idField.setText(String.valueOf(classToEdit.getClassId()));
            classNameField.setText(classToEdit.getClassName());
            // Select the correct course and teacher in the ComboBoxes
            selectComboBoxItem(courseComboBox, classToEdit.getCourse());
            selectComboBoxItem(teacherComboBox, classToEdit.getPrimaryTeacher());
            yearField.setText(classToEdit.getAcademicYear());
            semesterField.setText(classToEdit.getSemester());
            capacitySpinner.setValue(classToEdit.getMaxCapacity());
        } else {
            // Set default selections if needed
            if (courseComboBox.getItemCount() > 0) courseComboBox.setSelectedIndex(0);
            if (teacherComboBox.getItemCount() > 0) teacherComboBox.setSelectedIndex(0);
            capacitySpinner.setValue(20); // Default capacity
        }
    }

    // Helper to select item in ComboBox based on object equality (requires proper equals method)
    private <T> void selectComboBoxItem(JComboBox<T> comboBox, T itemToSelect) {
        if (itemToSelect == null) {
            comboBox.setSelectedIndex(-1); // No selection
            return;
        }
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            // Use Objects.equals for null-safe comparison
            if (Objects.equals(comboBox.getItemAt(i), itemToSelect)) {
                comboBox.setSelectedIndex(i);
                return;
            }
        }
        comboBox.setSelectedIndex(-1); // Item not found in the list
    }


    private void configureDialog() {
        pack();
        setMinimumSize(new Dimension(500, 350)); // Adjust minimum size
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void saveClass() {
        // --- Input Validation ---
        String className = classNameField.getText().trim();
        Course selectedCourse = (Course) courseComboBox.getSelectedItem();
        Teacher selectedTeacher = (Teacher) teacherComboBox.getSelectedItem();
        String year = yearField.getText().trim();
        String semester = semesterField.getText().trim();
        int capacity = (int) capacitySpinner.getValue();


        if (!ValidationUtils.isNotEmpty(className)) {
            UIUtils.showWarningMessage(this, "Validation Error", "Class Name cannot be empty.");
            classNameField.requestFocusInWindow();
            return;
        }
        if (selectedCourse == null) {
            UIUtils.showWarningMessage(this, "Validation Error", "Please select a Course.");
            courseComboBox.requestFocusInWindow();
            return;
        }
        if (selectedTeacher == null) {
            UIUtils.showWarningMessage(this, "Validation Error", "Please select a Primary Teacher.");
            teacherComboBox.requestFocusInWindow();
            return;
        }
        if (capacity <= 0) {
            UIUtils.showWarningMessage(this, "Validation Error", "Maximum Capacity must be positive.");
            capacitySpinner.requestFocusInWindow();
            return;
        }


        // --- Create or Update EduClass Object ---
        EduClass eduClass = isEditMode ? classToEdit : new EduClass();
        eduClass.setClassName(className);
        eduClass.setCourse(selectedCourse);
        eduClass.setPrimaryTeacher(selectedTeacher);
        eduClass.setAcademicYear(year);
        eduClass.setSemester(semester);
        eduClass.setMaxCapacity(capacity);
        // Student list is managed separately (enroll/unenroll)
        // ID handled by DAO or exists already


        // --- Call Controller ---
        boolean success;
        if (isEditMode) {
            // Preserve enrolled students when updating
            // EduClassController's update should ideally handle this,
            // but we ensure the student list isn't lost here if needed.
            if (classToEdit != null) {
                eduClass.setStudentIds(classToEdit.getStudentIds()); // Keep existing students
            }
            success = controller.updateEduClass(eduClass);
        } else {
            success = controller.addEduClass(eduClass);
        }

        if (success) {
            dispose(); // Close dialog on success
        }
        // Controller shows messages
    }
}