package com.eduzk.view.panels;

import com.eduzk.controller.ScheduleController;
import com.eduzk.model.entities.Schedule;
import com.eduzk.utils.DateUtils;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.components.CustomDatePicker;
import com.eduzk.view.dialogs.ScheduleDialog;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Vector;
import javax.swing.Icon;
import java.net.URL;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.Timer;

public class SchedulePanel extends JPanel {
    private ScheduleController controller;
    private JTable scheduleTable;
    private DefaultTableModel tableModel;
    private JButton addButton, editButton, deleteButton, filterButton;
    private JButton refreshButton;
    private CustomDatePicker startDatePicker;
    private CustomDatePicker endDatePicker;
    private TableRowSorter<DefaultTableModel> sorter;
    private Timer statusUpdateTimer;
    private final Icon greenDot = new GreenDotIcon(7);
    private final Icon redDot = new RedDotIcon(7);
    private final Icon grayDot = new GrayDotIcon(7);
    private final int ID_COL_MODEL = 0;
    private final int DATE_COL_MODEL = 1;
    private final int START_TIME_COL_MODEL = 2;
    private final int END_TIME_COL_MODEL = 3;
    private final int CLASS_NAME_COL_MODEL = 4;
    private final int TEACHER_NAME_COL_MODEL = 5;
    private final int ROOM_NAME_COL_MODEL = 6;
    private final int STATUS_COL_MODEL = 7;
    private final DateTimeFormatter timeFormatter = DateUtils.TIME_FORMATTER;
    private final DateTimeFormatter dateFormatter = DateUtils.DATE_FORMATTER;

    public SchedulePanel(ScheduleController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
        setupLayout();
        setupActions();
        startStatusUpdater();
    }

    public void setController(ScheduleController controller) {
        this.controller = controller;
        loadInitialSchedule();
    }

    private void loadInitialSchedule() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        startDatePicker.setDate(startOfWeek);
        endDatePicker.setDate(endOfWeek);
        refreshScheduleView();
    }

    private void initComponents() {
        // Date Pickers
        startDatePicker = new CustomDatePicker(LocalDate.now().minusDays(7));
        endDatePicker = new CustomDatePicker(LocalDate.now());

        // Table Model
        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Date", "Start", "End", "Class Name", "Teacher", "Room", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == STATUS_COL_MODEL) {
                    return Icon.class;
                }
                if (columnIndex == ID_COL_MODEL) return Integer.class;
                return String.class;
            }
        };
        scheduleTable = new JTable(tableModel);
        scheduleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scheduleTable.setAutoCreateRowSorter(true);
        sorter = (TableRowSorter<DefaultTableModel>) scheduleTable.getRowSorter();
        scheduleTable.setRowSorter(sorter);
        setupTableRenderersAndWidths();

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
        int iconSize = 20;

        addButton = new JButton("Add Schedule");
        Icon addIcon = loadSVGIconButton("/icons/add.svg", iconSize);
        if (addIcon != null) addButton.setIcon(addIcon);

        editButton = new JButton("Edit Schedule");
        Icon editIcon = loadSVGIconButton("/icons/edit.svg", iconSize);
        if (editIcon != null) editButton.setIcon(editIcon);

        deleteButton = new JButton("Delete Schedule");
        Icon deleteIcon = loadSVGIconButton("/icons/delete.svg", iconSize);
        if (deleteIcon != null) deleteButton.setIcon(deleteIcon);

        refreshButton = new JButton("Refresh");
        Icon refreshIcon = loadSVGIconButton("/icons/refresh.svg", iconSize);
        if (refreshIcon != null) refreshButton.setIcon(refreshIcon);
        refreshButton.setToolTipText("Reload schedule data from storage");

        filterButton = new JButton("Load Schedule");

    }

    private void setupLayout() {
        // Top Panel
        JPanel topPanel = new JPanel(new BorderLayout(20, 0));
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
                String desc = tableModel.getValueAt(modelRow, 1) + " " +
                        tableModel.getValueAt(modelRow, 2) + "-" +
                        tableModel.getValueAt(modelRow, 3) + " (" +
                        tableModel.getValueAt(modelRow, 4) + ")";

                if (UIUtils.showConfirmDialog(this, "Confirm Deletion", "Delete schedule entry?\n" + desc)) {
                    if (controller != null) {
                        controller.deleteSchedule(scheduleId);
                    }
                }
            } else {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select a schedule entry to delete.");
            }
        });
        refreshButton.addActionListener(e -> {
            System.out.println("SchedulePanel: Refresh button clicked.");
            refreshScheduleView();
            UIUtils.showInfoMessage(this,"Refreshed", "Schedule list updated.");
        });

    }

    private void openScheduleDialog(Schedule schedule) {
        if (controller == null) {
            UIUtils.showErrorMessage(this, "Error", "Schedule Controller is not initialized.");
            return;
        }
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        ScheduleDialog dialog = new ScheduleDialog((Frame) parentWindow, controller, schedule);
        dialog.setVisible(true);
    }

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
        tableModel.setRowCount(0);
        if (schedules != null && controller != null) {
            for (Schedule schedule : schedules) {
                Vector<Object> row = new Vector<>();
                row.add(schedule.getScheduleId());
                row.add(DateUtils.formatDate(schedule.getDate()));
                row.add(DateUtils.formatTime(schedule.getStartTime()));
                row.add(DateUtils.formatTime(schedule.getEndTime()));
                row.add(controller.getClassNameById(schedule.getClassId()));
                row.add(controller.getTeacherNameById(schedule.getTeacherId()));
                row.add(controller.getRoomNameById(schedule.getRoomId()));
                row.add(null);
                tableModel.addRow(row);
            }
        }
        if(scheduleTable != null) scheduleTable.repaint();
    }

    public void setAdminControlsEnabled(boolean isAdmin) {
        addButton.setVisible(isAdmin);
        editButton.setVisible(isAdmin);
        deleteButton.setVisible(isAdmin);
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
    private void setupTableRenderersAndWidths() {
        if (scheduleTable == null) return;
        TableColumnModel columnModel = scheduleTable.getColumnModel();
        // Cột ID
        TableColumn idCol = columnModel.getColumn(ID_COL_MODEL);
        idCol.setPreferredWidth(40);
        idCol.setMaxWidth(60);
        // Cột Status
        TableColumn statusCol = columnModel.getColumn(STATUS_COL_MODEL);
        statusCol.setCellRenderer(new ScheduleStatusRenderer());
        statusCol.setPreferredWidth(60);
        statusCol.setMaxWidth(80);
        statusCol.setMinWidth(40);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        columnModel.getColumn(DATE_COL_MODEL).setCellRenderer(centerRenderer);
        columnModel.getColumn(START_TIME_COL_MODEL).setCellRenderer(centerRenderer);
        columnModel.getColumn(END_TIME_COL_MODEL).setCellRenderer(centerRenderer);
        columnModel.getColumn(ROOM_NAME_COL_MODEL).setCellRenderer(centerRenderer);

        columnModel.getColumn(DATE_COL_MODEL).setPreferredWidth(90);
        columnModel.getColumn(START_TIME_COL_MODEL).setPreferredWidth(60);
        columnModel.getColumn(END_TIME_COL_MODEL).setPreferredWidth(60);
        columnModel.getColumn(CLASS_NAME_COL_MODEL).setPreferredWidth(180);
        columnModel.getColumn(TEACHER_NAME_COL_MODEL).setPreferredWidth(150);
        columnModel.getColumn(ROOM_NAME_COL_MODEL).setPreferredWidth(120);
    }
    private void startStatusUpdater() {
        if (statusUpdateTimer == null) {
            statusUpdateTimer = new Timer(30000, e -> updateScheduleStatuses());
            statusUpdateTimer.setInitialDelay(1000);
            statusUpdateTimer.start();
            System.out.println("Schedule Status Updater Timer started.");
        } else if (!statusUpdateTimer.isRunning()) {
            statusUpdateTimer.start();
            System.out.println("Schedule Status Updater Timer restarted.");
        }
    }

    public void stopStatusUpdater() {
        if (statusUpdateTimer != null && statusUpdateTimer.isRunning()) {
            statusUpdateTimer.stop();
            System.out.println("Schedule Status Updater Timer stopped.");
        }
    }

    private void updateScheduleStatuses() {
        if (scheduleTable != null && scheduleTable.isVisible() && scheduleTable.getRowCount() > 0) {
            scheduleTable.repaint();
        }
    }

    //LOP RIENG NACDANH
    private class ScheduleStatusRenderer extends DefaultTableCellRenderer {
        public ScheduleStatusRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setText(null);
            label.setIcon(grayDot);
            label.setToolTipText("Scheduled");

            try {
                int modelRow = table.convertRowIndexToModel(row);
                Object dateObj = table.getModel().getValueAt(modelRow, DATE_COL_MODEL);
                Object startTimeObj = table.getModel().getValueAt(modelRow, START_TIME_COL_MODEL);
                Object endTimeObj = table.getModel().getValueAt(modelRow, END_TIME_COL_MODEL);
                if (dateObj instanceof String && startTimeObj instanceof String && endTimeObj instanceof String) {
                    String dateStr = (String) dateObj;
                    String startTimeStr = (String) startTimeObj;
                    String endTimeStr = (String) endTimeObj;

                    if (!dateStr.isEmpty() && !startTimeStr.isEmpty() && !endTimeStr.isEmpty()) {
                        LocalDate scheduleDate = LocalDate.parse(dateStr, dateFormatter);
                        LocalTime startTime = LocalTime.parse(startTimeStr, timeFormatter);
                        LocalTime endTime = LocalTime.parse(endTimeStr, timeFormatter);

                        LocalDateTime startDateTime = scheduleDate.atTime(startTime);
                        LocalDateTime endDateTime = scheduleDate.atTime(endTime);
                        LocalDateTime now = LocalDateTime.now();

                        if (now.isAfter(endDateTime)) {
                            label.setIcon(redDot);
                            label.setToolTipText("Finished");
                        } else if (now.isAfter(startDateTime)) {
                            label.setIcon(greenDot);
                            label.setToolTipText("In Use");
                        }
                    } else {
                        label.setToolTipText("Missing Time Data");
                    }
                } else {
                    label.setToolTipText("Invalid Data Type");
                }
            } catch (DateTimeParseException e) {
                System.err.println("Error parsing date/time in ScheduleStatusRenderer at row " + row + ": " + e.getMessage());
                label.setToolTipText("Parse Error");
            } catch (Exception e) {
                System.err.println("Error in ScheduleStatusRenderer at row " + row + ": " + e.getMessage());
                e.printStackTrace();
                label.setToolTipText("Error");
            }
            return label;
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        stopStatusUpdater();
    }
    abstract class AbstractDotIcon implements Icon {
        private final Color color;
        private final int size;
        public AbstractDotIcon(Color color, int size) { this.color = color; this.size = size; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            int drawX = x + (getIconWidth() - size) / 2;
            int drawY = y + (getIconHeight() - size) / 2;
            g2.fillOval(drawX, drawY, size, size);
            g2.dispose();
        }
        @Override public int getIconWidth() { return size + 4; }
        @Override public int getIconHeight() { return size + 4; }
    }
    class GreenDotIcon extends AbstractDotIcon { public GreenDotIcon(int size) { super(new Color(83, 214, 83), size); } }
    class RedDotIcon extends AbstractDotIcon { public RedDotIcon(int size) { super(new Color(214, 94, 94), size); } }
    class GrayDotIcon extends AbstractDotIcon { public GrayDotIcon(int size) { super(Color.LIGHT_GRAY, size); } }
}
