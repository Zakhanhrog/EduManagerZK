package com.eduzk.view.dialogs;

import com.eduzk.controller.ScheduleController;
import com.eduzk.model.entities.EduClass;
import com.eduzk.model.entities.Room;
import com.eduzk.model.entities.Schedule;
import com.eduzk.model.entities.Teacher;
import com.eduzk.utils.UIUtils;
import com.eduzk.utils.ValidationUtils;
import com.eduzk.view.components.CustomDatePicker;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class ScheduleDialog extends JDialog {

    private final ScheduleController controller;
    private final Schedule scheduleToEdit;
    private final boolean isEditMode;

    // Data for ComboBoxes (fetched via controller)
    private List<EduClass> availableClasses;
    private List<Teacher> availableTeachers;
    private List<Room> availableRooms;


    // UI Components
    private JTextField idField;
    private JComboBox<EduClass> classComboBox;
    private JComboBox<Teacher> teacherComboBox;
    private JComboBox<Room> roomComboBox;
    private CustomDatePicker datePicker;
    private JSpinner startTimeSpinner;
    private JSpinner endTimeSpinner;
    private JButton saveButton;
    private JButton cancelButton;

    public ScheduleDialog(Frame owner, ScheduleController controller, Schedule schedule) {
        super(owner, true);
        this.controller = controller;
        this.scheduleToEdit = schedule;
        this.isEditMode = (schedule != null);

        // Fetch data needed for dropdowns
        fetchComboBoxData();

        setTitle(isEditMode ? "Edit Schedule Entry" : "Add Schedule Entry");
        initComponents();
        setupLayout();
        setupActions();
        populateFields();
        configureDialog();
    }

    private void fetchComboBoxData() {
        if (controller != null) {
            this.availableClasses = controller.getAllClassesForSelection();
            this.availableTeachers = controller.getAllTeachersForSelection();
            this.availableRooms = controller.getAllRoomsForSelection();
        } else {
            // Handle error or provide empty lists
            this.availableClasses = List.of();
            this.availableTeachers = List.of();
            this.availableRooms = List.of();
            System.err.println("ScheduleDialog: Controller is null during data fetch!");
        }
    }

    private void initComponents() {
        idField = new JTextField(5);
        idField.setEditable(false);

        classComboBox = new JComboBox<>(new Vector<>(availableClasses));
        teacherComboBox = new JComboBox<>(new Vector<>(availableTeachers));
        roomComboBox = new JComboBox<>(new Vector<>(availableRooms));

        // Renderers for better display
        classComboBox.setRenderer(createClassRenderer());
        teacherComboBox.setRenderer(createTeacherRenderer());
        roomComboBox.setRenderer(createRoomRenderer());


        datePicker = new CustomDatePicker();

        // Time Spinners (using Date model for easy time selection)
        SpinnerDateModel startModel = new SpinnerDateModel(getDefaultTime(8), null, null, Calendar.MINUTE); // Default 8:00 AM
        startTimeSpinner = new JSpinner(startModel);
        JSpinner.DateEditor startTimeEditor = new JSpinner.DateEditor(startTimeSpinner, "HH:mm"); // 24hr format
        startTimeSpinner.setEditor(startTimeEditor);

        SpinnerDateModel endModel = new SpinnerDateModel(getDefaultTime(9), null, null, Calendar.MINUTE); // Default 9:00 AM
        endTimeSpinner = new JSpinner(endModel);
        JSpinner.DateEditor endTimeEditor = new JSpinner.DateEditor(endTimeSpinner, "HH:mm");
        endTimeSpinner.setEditor(endTimeEditor);


        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
    }

    // Helper for default spinner time
    private Date getDefaultTime(int hour) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }


    // --- ComboBox Renderers ---
    private ListCellRenderer<Object> createClassRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof EduClass) {
                    setText(((EduClass) value).getClassName()); // Or use toString()
                } else if (value == null && index == -1) setText("");
                return this;
            }
        };
    }
    private ListCellRenderer<Object> createTeacherRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Teacher) {
                    setText(((Teacher) value).getFullName());
                } else if (value == null && index == -1) setText("");
                return this;
            }
        };
    }
    private ListCellRenderer<Object> createRoomRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Room) {
                    setText(((Room) value).toString()); // Use Room's toString
                } else if (value == null && index == -1) setText("");
                return this;
            }
        };
    }
    // --- End Renderers ---


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

        // Row: Class
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Class*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(classComboBox, gbc);
        currentRow++;

        // Row: Teacher
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Teacher*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(teacherComboBox, gbc);
        currentRow++;

        // Row: Room
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Room*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        formPanel.add(roomComboBox, gbc);
        currentRow++;

        // Row: Date & Times
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Date*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.4;
        formPanel.add(datePicker, gbc);

        gbc.gridx = 2; gbc.gridy = currentRow; gbc.gridwidth = 2; // Span 2 columns for time range
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); // Panel for time range
        timePanel.add(new JLabel("Time*:"));
        timePanel.add(startTimeSpinner);
        timePanel.add(new JLabel("-"));
        timePanel.add(endTimeSpinner);
        formPanel.add(timePanel, gbc);
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
        saveButton.addActionListener(e -> saveSchedule());
        cancelButton.addActionListener(e -> dispose());
    }

    private void populateFields() {
        if (isEditMode && scheduleToEdit != null) {
            idField.setText(String.valueOf(scheduleToEdit.getScheduleId()));
            // Select items in combo boxes
            selectComboBoxItemById(classComboBox, scheduleToEdit.getClassId());
            selectComboBoxItemById(teacherComboBox, scheduleToEdit.getTeacherId());
            selectComboBoxItemById(roomComboBox, scheduleToEdit.getRoomId());

            datePicker.setDate(scheduleToEdit.getDate());
            // Set time spinners
            if (scheduleToEdit.getStartTime() != null) {
                startTimeSpinner.setValue(Date.from(scheduleToEdit.getStartTime().atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant()));
            }
            if (scheduleToEdit.getEndTime() != null) {
                endTimeSpinner.setValue(Date.from(scheduleToEdit.getEndTime().atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant()));
            }

        } else {
            // Set defaults for add mode if needed
            if (!availableClasses.isEmpty()) classComboBox.setSelectedIndex(0);
            if (!availableTeachers.isEmpty()) teacherComboBox.setSelectedIndex(0);
            if (!availableRooms.isEmpty()) roomComboBox.setSelectedIndex(0);
            datePicker.setDate(LocalDate.now()); // Default to today
        }
    }

    // Helper to select item in ComboBox based on ID
    private <T> void selectComboBoxItemById(JComboBox<T> comboBox, int idToSelect) {
        if (idToSelect <= 0) {
            comboBox.setSelectedIndex(-1);
            return;
        }
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            T item = comboBox.getItemAt(i);
            int itemId = -1;
            // Get ID based on type
            if (item instanceof EduClass) itemId = ((EduClass) item).getClassId();
            else if (item instanceof Teacher) itemId = ((Teacher) item).getTeacherId();
            else if (item instanceof Room) itemId = ((Room) item).getRoomId();

            if (itemId == idToSelect) {
                comboBox.setSelectedIndex(i);
                return;
            }
        }
        comboBox.setSelectedIndex(-1); // Item not found
    }


    private void configureDialog() {
        pack();
        setMinimumSize(new Dimension(550, 350)); // Adjust minimum size
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private LocalTime getTimeFromSpinner(JSpinner spinner) {
        Object value = spinner.getValue();
        if (value instanceof Date) {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) value);
            // Important: Extract only hour and minute
            return LocalTime.of(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        }
        return null; // Or throw an error
    }


    private void saveSchedule() {
        // --- Get Selected Data ---
        EduClass selectedClass = (EduClass) classComboBox.getSelectedItem();
        Teacher selectedTeacher = (Teacher) teacherComboBox.getSelectedItem();
        Room selectedRoom = (Room) roomComboBox.getSelectedItem();
        LocalDate selectedDate = datePicker.getDate();
        LocalTime startTime = getTimeFromSpinner(startTimeSpinner);
        LocalTime endTime = getTimeFromSpinner(endTimeSpinner);

        // --- Input Validation ---
        if (selectedClass == null) {
            UIUtils.showWarningMessage(this, "Validation Error", "Please select a Class.");
            classComboBox.requestFocusInWindow(); return;
        }
        if (selectedTeacher == null) {
            UIUtils.showWarningMessage(this, "Validation Error", "Please select a Teacher.");
            teacherComboBox.requestFocusInWindow(); return;
        }
        if (selectedRoom == null) {
            UIUtils.showWarningMessage(this, "Validation Error", "Please select a Room.");
            roomComboBox.requestFocusInWindow(); return;
        }
        if (!ValidationUtils.isValidDate(selectedDate)) { // Should not happen with picker, but check
            UIUtils.showWarningMessage(this, "Validation Error", "Please select a valid Date.");
            datePicker.requestFocusInWindow(); return;
        }
        if (!ValidationUtils.isValidTime(startTime) || !ValidationUtils.isValidTime(endTime)) {
            UIUtils.showWarningMessage(this, "Validation Error", "Please select valid Start and End Times.");
            return; // Focus start time spinner?
        }
        if (!ValidationUtils.isValidTimeRange(startTime, endTime)) {
            UIUtils.showWarningMessage(this, "Validation Error", "End Time cannot be before Start Time.");
            endTimeSpinner.requestFocusInWindow();
            return;
        }


        // --- Create or Update Schedule Object ---
        Schedule schedule = isEditMode ? scheduleToEdit : new Schedule();
        schedule.setClassId(selectedClass.getClassId());
        schedule.setTeacherId(selectedTeacher.getTeacherId());
        schedule.setRoomId(selectedRoom.getRoomId());
        schedule.setDate(selectedDate);
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        // ID handled by DAO or exists already


        // --- Call Controller ---
        boolean success = false;
        try {
            if (isEditMode) {
                success = controller.updateSchedule(schedule);
            } else {
                success = controller.addSchedule(schedule);
            }

            if (success) {
                dispose(); // Close dialog on success
            }
            // Controller shows success message or conflict exception message
        } catch (Exception e) {
            // Catch other unexpected errors during save
            System.err.println("Unexpected error saving schedule: " + e.getMessage());
            UIUtils.showErrorMessage(this, "Error", "An unexpected error occurred while saving the schedule.");
        }
    }
}