package com.eduzk.view.panels;

import com.eduzk.controller.ScheduleController;
import com.eduzk.model.entities.Schedule;
import com.eduzk.utils.DateUtils;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.components.CustomDatePicker;
import com.eduzk.view.dialogs.ScheduleDialog;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Vector;
import javax.swing.Icon;
import java.net.URL;
import com.formdev.flatlaf.extras.FlatSVGIcon;

public class SchedulePanel extends JPanel {

    private ScheduleController controller;
    private JTable scheduleTable;
    private DefaultTableModel tableModel;
    private JButton addButton, editButton, deleteButton, filterButton;
    private JButton refreshButton;
    private CustomDatePicker startDatePicker;
    private CustomDatePicker endDatePicker;
    private TableRowSorter<DefaultTableModel> sorter;

    public SchedulePanel(ScheduleController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
        setupLayout();
        setupActions();
    }

    public void setController(ScheduleController controller) {
        this.controller = controller;
        // Initial load (e.g., for the current week or month)
        loadInitialSchedule();
    }

    private void loadInitialSchedule() {
        // Default: Show schedule for the current week
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        startDatePicker.setDate(startOfWeek);
        endDatePicker.setDate(endOfWeek);
        refreshScheduleView(); // Load data for this range
    }

    private void initComponents() {
        // Date Pickers
        startDatePicker = new CustomDatePicker(LocalDate.now().minusDays(7)); // Default start: 1 week ago
        endDatePicker = new CustomDatePicker(LocalDate.now()); // Default end: today

        // Table Model
        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Date", "Start", "End", "Class Name", "Teacher", "Room"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            // Could add Class<?> overrides for Date/Time if needed for sorting
        };
        scheduleTable = new JTable(tableModel);
        scheduleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scheduleTable.setAutoCreateRowSorter(true);
        sorter = (TableRowSorter<DefaultTableModel>) scheduleTable.getRowSorter();

        // Adjust column widths
        TableColumn idCol = scheduleTable.getColumnModel().getColumn(0);
        idCol.setPreferredWidth(40); idCol.setMaxWidth(60);
        TableColumn dateCol = scheduleTable.getColumnModel().getColumn(1);
        dateCol.setPreferredWidth(90);
        TableColumn startCol = scheduleTable.getColumnModel().getColumn(2);
        startCol.setPreferredWidth(60);
        TableColumn endCol = scheduleTable.getColumnModel().getColumn(3);
        endCol.setPreferredWidth(60);
        TableColumn classCol = scheduleTable.getColumnModel().getColumn(4);
        classCol.setPreferredWidth(180);
        TableColumn teacherCol = scheduleTable.getColumnModel().getColumn(5);
        teacherCol.setPreferredWidth(150);
        TableColumn roomCol = scheduleTable.getColumnModel().getColumn(6);
        roomCol.setPreferredWidth(120);


        // Buttons
        int iconSize = 16;

        addButton = new JButton("Add Entry");
        Icon addIcon = loadSVGIconButton("/icons/add.svg", iconSize);
        if (addIcon != null) addButton.setIcon(addIcon);

        editButton = new JButton("Edit Entry");
        Icon editIcon = loadSVGIconButton("/icons/edit.svg", iconSize);
        if (editIcon != null) editButton.setIcon(editIcon);

        deleteButton = new JButton("Delete Entry");
        Icon deleteIcon = loadSVGIconButton("/icons/delete.svg", iconSize);
        if (deleteIcon != null) deleteButton.setIcon(deleteIcon);

        refreshButton = new JButton("Refresh");
        Icon refreshIcon = loadSVGIconButton("/icons/refresh.svg", iconSize);
        if (refreshIcon != null) refreshButton.setIcon(refreshIcon);
        refreshButton.setToolTipText("Reload schedule data from storage");

        filterButton = new JButton("Load Schedule");

    }

    private void setupLayout() {
        // --- Top Panel (Filters and Actions) ---
        JPanel topPanel = new JPanel(new BorderLayout(20, 0)); // Add gap between filters and actions
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("From:"));
        filterPanel.add(startDatePicker);
        filterPanel.add(new JLabel("To:"));
        filterPanel.add(endDatePicker);
        filterPanel.add(filterButton);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.add(refreshButton);
        actionPanel.add(addButton);
        actionPanel.add(editButton);
        actionPanel.add(deleteButton);

        topPanel.add(filterPanel, BorderLayout.WEST);
        topPanel.add(actionPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // --- Center Panel (Table) ---
        JScrollPane scrollPane = new JScrollPane(scheduleTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void setupActions() {
        filterButton.addActionListener(e -> refreshScheduleView());

        addButton.addActionListener(e -> openScheduleDialog(null));

        editButton.addActionListener(e -> {
            int selectedRow = scheduleTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = scheduleTable.convertRowIndexToModel(selectedRow);
                int scheduleId = (int) tableModel.getValueAt(modelRow, 0);
                Schedule scheduleToEdit = controller.getScheduleById(scheduleId);
                if (scheduleToEdit != null) {
                    openScheduleDialog(scheduleToEdit);
                } else {
                    UIUtils.showErrorMessage(this, "Error", "Could not retrieve schedule details for editing.");
                }
            } else {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select a schedule entry to edit.");
            }
        });

        deleteButton.addActionListener(e -> {
            int selectedRow = scheduleTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = scheduleTable.convertRowIndexToModel(selectedRow);
                int scheduleId = (int) tableModel.getValueAt(modelRow, 0);
                String desc = tableModel.getValueAt(modelRow, 1) + " " + // Date
                        tableModel.getValueAt(modelRow, 2) + "-" + // Start time
                        tableModel.getValueAt(modelRow, 3) + " (" + // End time
                        tableModel.getValueAt(modelRow, 4) + ")"; // Class name

                if (UIUtils.showConfirmDialog(this, "Confirm Deletion", "Delete schedule entry?\n" + desc)) {
                    if (controller != null) {
                        controller.deleteSchedule(scheduleId);
                        // Refresh handled by controller
                    }
                }
            } else {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select a schedule entry to delete.");
            }
        });
        refreshButton.addActionListener(e -> {
            System.out.println("SchedulePanel: Refresh button clicked.");
            refreshScheduleView(); // Gọi lại chính phương thức refresh của panel này
            UIUtils.showInfoMessage(this,"Refreshed", "Schedule list updated."); // Thông báo (tùy chọn)
        });

    }

    private void openScheduleDialog(Schedule schedule) {
        if (controller == null) {
            UIUtils.showErrorMessage(this, "Error", "Schedule Controller is not initialized.");
            return;
        }
        // Dialog needs lists for dropdowns
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        ScheduleDialog dialog = new ScheduleDialog((Frame) parentWindow, controller, schedule);
        dialog.setVisible(true);
        // Refresh potentially handled by controller after save
        // refreshScheduleView(); // Or refresh here after dialog closes, if controller doesn't
    }

    // Called by controller or filter button action
    public void refreshScheduleView() {
        if (controller == null) return;

        LocalDate startDate = startDatePicker.getDate();
        LocalDate endDate = endDatePicker.getDate();

        if (startDate == null || endDate == null) {
            UIUtils.showWarningMessage(this, "Invalid Date", "Please select valid start and end dates.");
            return;
        }
        if (endDate.isBefore(startDate)) {
            UIUtils.showWarningMessage(this, "Invalid Range", "End date cannot be before start date.");
            return;
        }

        List<Schedule> schedules = controller.getSchedulesByDateRange(startDate, endDate);
        populateTable(schedules);
    }

    private void populateTable(List<Schedule> schedules) {
        tableModel.setRowCount(0); // Clear existing data
        if (schedules != null && controller != null) {
            for (Schedule schedule : schedules) {
                Vector<Object> row = new Vector<>();
                row.add(schedule.getScheduleId());
                row.add(DateUtils.formatDate(schedule.getDate()));
                row.add(DateUtils.formatTime(schedule.getStartTime()));
                row.add(DateUtils.formatTime(schedule.getEndTime()));
                // Fetch names using controller helper methods to avoid loading full objects here
                row.add(controller.getClassNameById(schedule.getClassId()));
                row.add(controller.getTeacherNameById(schedule.getTeacherId()));
                row.add(controller.getRoomNameById(schedule.getRoomId()));
                tableModel.addRow(row);
            }
        }
    }

    public void setAdminControlsEnabled(boolean isAdmin) {
        addButton.setVisible(isAdmin); // Hoặc setEnabled(isAdmin)
        editButton.setVisible(isAdmin);
        deleteButton.setVisible(isAdmin);
        // Các nút khác (Search) có thể luôn hiển thị/enabled
//         searchButton.setEnabled(true);
//         searchField.setEnabled(true);
    }

    private Icon loadSVGIconButton(String path, int size) {
        if (path == null || path.isEmpty()) return null;
        try {
            URL iconUrl = getClass().getResource(path);
            if (iconUrl != null) {
                return new FlatSVGIcon(iconUrl).derive(size, size);
            } else {
                System.err.println("Warning: Button SVG icon resource not found at: " + path + " in " + getClass().getSimpleName());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error loading/parsing SVG button icon from path: " + path + " - " + e.getMessage());
            return null;
        }
    }
}