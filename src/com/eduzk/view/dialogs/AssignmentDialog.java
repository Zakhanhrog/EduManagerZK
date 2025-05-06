package com.eduzk.view.dialogs;

import com.eduzk.model.entities.Assignment;
import com.eduzk.model.entities.EduClass;
import com.eduzk.utils.UIUtils;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.Objects;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AssignmentDialog extends JDialog {

    private JTextField titleField;
    private JTextArea descriptionArea;
    private DatePicker dueDatePicker;
    private JTextField dueTimeField;
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private JLabel classLabel;

    private JButton saveButton;
    private JButton cancelButton;

    private Assignment currentAssignment;
    private boolean saved = false;
    private EduClass associatedClass;

    public AssignmentDialog(Frame owner, String dialogTitle, EduClass associatedClass, Assignment assignmentToEdit) {
        super(owner, dialogTitle, true);
        this.associatedClass = associatedClass;

        this.currentAssignment = (assignmentToEdit == null) ? new Assignment() : assignmentToEdit;
        if (assignmentToEdit == null && associatedClass != null) {
            this.currentAssignment.setEduClassId(associatedClass.getClassId());
        }

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
            dueTimeField.setText("");
        }
        if (assignmentToEdit != null && this.currentAssignment.isOverdue()) {
            setFieldsEditable(false);
            saveButton.setEnabled(false);
            saveButton.setToolTipText("Assignment is overdue and cannot be saved.");
            cancelButton.setText("Close"); // Đổi nút Cancel thành Close
            setTitle(dialogTitle + " (Đã quá hạn - Chỉ xem)");
        } else {
            setFieldsEditable(true);
            saveButton.setEnabled(true);
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

        dueTimeField = new JTextField(5); // Kích thước cho HH:MM
        dueTimeField.setToolTipText("Enter time in HH:MM format (e.g., 14:30)");

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
        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Due Date (YYYY-MM-DD):"), gbc);
        JPanel dueDateTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        dueDateTimePanel.add(dueDatePicker);
        dueDateTimePanel.add(Box.createHorizontalStrut(5));
        dueDateTimePanel.add(new JLabel("Time (HH:MM):"));
        dueDateTimePanel.add(dueTimeField);
        gbc.gridx = 1; gbc.gridy = 2;
        inputPanel.add(dueDateTimePanel, gbc);

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
    private void setFieldsEditable(boolean editable) {
        titleField.setEditable(editable);
        descriptionArea.setEditable(editable);
        dueDatePicker.setEnabled(editable); // DatePicker dùng setEnabled
        dueTimeField.setEditable(editable);
    }

    private void setupActions() {
        saveButton.addActionListener(e -> saveAssignment());
        cancelButton.addActionListener(e -> dispose()); // Đóng dialog
    }

    private void populateFields(Assignment assignment) {
        titleField.setText(assignment.getTitle());
        descriptionArea.setText(assignment.getDescription());
        if (assignment.getDueDateTime() != null) {
            dueDatePicker.setDate(assignment.getDueDateTime().toLocalDate());
            dueTimeField.setText(assignment.getDueDateTime().toLocalTime().format(timeFormatter));
        } else {
            dueDatePicker.clear();
            dueTimeField.setText("");
        }
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

        LocalDate selectedDate = dueDatePicker.getDate();
        String timeString = dueTimeField.getText().trim();
        LocalDateTime dueDateTimeToSave = null;

        if (selectedDate != null && !timeString.isEmpty()) {
            try {
                LocalTime selectedTime = LocalTime.parse(timeString, timeFormatter);
                dueDateTimeToSave = LocalDateTime.of(selectedDate, selectedTime);
            } catch (DateTimeParseException ex) {
                UIUtils.showErrorMessage(this, "Invalid Time Format", "Please enter time in HH:MM format (e.g., 14:30) or leave both date and time empty.");
                dueTimeField.requestFocus();
                return;
            }
        } else if (selectedDate != null && timeString.isEmpty()) {
            UIUtils.showWarningMessage(this, "Missing Time", "Please enter a time for the selected due date, or clear the date as well.");
            dueTimeField.requestFocus();
            return;
        } else if (selectedDate == null && !timeString.isEmpty()) {
            UIUtils.showWarningMessage(this, "Missing Date", "Please select a date for the entered due time, or clear the time as well.");
            dueDatePicker.requestFocusInWindow();
            return;
        }

        currentAssignment.setTitle(title);
        currentAssignment.setDescription(description);
        currentAssignment.setDueDateTime(dueDateTimeToSave);
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