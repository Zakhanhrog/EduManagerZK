// src/com/eduzk/view/dialogs/AssignmentDialog.java
package com.eduzk.view.dialogs;

import com.eduzk.model.entities.Assignment;
import com.eduzk.model.entities.EduClass; // Cần EduClass để hiển thị tên lớp
import com.eduzk.utils.UIUtils;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.Objects;

public class AssignmentDialog extends JDialog {

    private JTextField titleField;
    private JTextArea descriptionArea;
    private DatePicker dueDatePicker;
    private JLabel classLabel; // Hiển thị lớp đang thao tác

    private JButton saveButton;
    private JButton cancelButton;

    private Assignment currentAssignment; // null nếu là thêm mới
    private boolean saved = false;

    public AssignmentDialog(Frame owner, String dialogTitle, EduClass associatedClass, Assignment assignmentToEdit) {
        super(owner, dialogTitle, true); // true = modal
        this.currentAssignment = assignmentToEdit;

        initComponents(associatedClass);
        setupLayout();
        setupActions();

        if (assignmentToEdit != null) {
            populateFields(assignmentToEdit);
        } else {
            // Mặc định cho thêm mới
            titleField.setText("");
            descriptionArea.setText("");
            dueDatePicker.clear();
        }

        pack(); // Điều chỉnh kích thước dialog
        setMinimumSize(new Dimension(450, 400));
        setLocationRelativeTo(owner); // Hiển thị giữa màn hình cha
    }

    private void initComponents(EduClass associatedClass) {
        titleField = new JTextField(30);
        descriptionArea = new JTextArea(5, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        // Date Picker configuration
        DatePickerSettings dateSettings = new DatePickerSettings();
        dateSettings.setFormatForDatesCommonEra("yyyy-MM-dd"); // Định dạng ngày
        dateSettings.setAllowKeyboardEditing(false); // Không cho nhập tay
        dueDatePicker = new DatePicker(dateSettings);

        // Label hiển thị lớp
        String className = (associatedClass != null) ? associatedClass.getClassName() : "N/A";
        classLabel = new JLabel("Class: " + className);
        classLabel.setFont(classLabel.getFont().deriveFont(Font.ITALIC));

        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Input Panel (using GridBagLayout for flexibility)
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Class Label
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // Span across 2 columns
        inputPanel.add(classLabel, gbc);
        gbc.gridwidth = 1; // Reset gridwidth

        // Title
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("Title: *"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0; // Allow title field to expand horizontally
        inputPanel.add(titleField, gbc);
        gbc.weightx = 0.0; // Reset weight

        // Due Date
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("Due Date:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(dueDatePicker, gbc);

        // Description
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.NORTHWEST; // Align label top-left
        inputPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH; // Allow text area to expand
        gbc.weighty = 1.0; // Allow text area to expand vertically
        gbc.gridheight = 2; // Span 2 rows vertically
        JScrollPane descriptionScrollPane = new JScrollPane(descriptionArea);
        descriptionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputPanel.add(descriptionScrollPane, gbc);
        gbc.weighty = 0.0; // Reset weight
        gbc.gridheight = 1; // Reset gridheight

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(inputPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupActions() {
        saveButton.addActionListener(e -> saveAssignment());
        cancelButton.addActionListener(e -> dispose()); // Đóng dialog
    }

    private void populateFields(Assignment assignment) {
        titleField.setText(assignment.getTitle());
        descriptionArea.setText(assignment.getDescription());
        dueDatePicker.setDate(assignment.getDueDate()); // Set LocalDate
    }

    private void saveAssignment() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            UIUtils.showWarningMessage(this, "Validation Error", "Assignment title cannot be empty.");
            titleField.requestFocus();
            return;
        }

        // Optional validation for due date (e.g., cannot be in the past)
        LocalDate dueDate = dueDatePicker.getDate();
        // if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
        //     UIUtils.showWarningMessage(this,"Validation Error", "Due date cannot be in the past.");
        //     dueDatePicker.requestFocus();
        //     return;
        // }

        String description = descriptionArea.getText().trim();

        if (currentAssignment == null) { // Creating new assignment
            currentAssignment = new Assignment();
        }

        currentAssignment.setTitle(title);
        currentAssignment.setDescription(description);
        currentAssignment.setDueDate(dueDate);
        // classId is set by the controller before calling save

        saved = true;
        dispose(); // Close the dialog
    }

    public boolean isSaved() {
        return saved;
    }

    public Assignment getAssignmentData() {
        return currentAssignment;
    }
}