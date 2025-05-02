package com.eduzk.view.panels;

import com.eduzk.controller.TeacherController;
import com.eduzk.model.entities.Teacher;
import com.eduzk.utils.DateUtils;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.dialogs.TeacherDialog;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.Icon;

public class TeacherPanel extends JPanel {

    private TeacherController controller;
    private JTable teacherTable;
    private DefaultTableModel tableModel;
    private JButton addButton, editButton, deleteButton, searchButton;
    private JButton refreshButton;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;
    private JButton importButton;

    public TeacherPanel(TeacherController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
        setupLayout();
        setupActions();
    }

    // Method to set the controller after instantiation (used by MainView)
    public void setController(TeacherController controller) {
        this.controller = controller;
        refreshTable(); // Initial load
    }

    private void initComponents() {
        // Table Model
        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Full Name", "Specialization", "Phone", "Email", "DOB", "Active"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Non-editable cells
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                // Correctly render boolean as checkbox
                if (columnIndex == 6) { // 'Active' column index
                    return Boolean.class;
                }
                return super.getColumnClass(columnIndex);
            }
        };
        teacherTable = new JTable(tableModel);
        teacherTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        teacherTable.setAutoCreateRowSorter(true);
        sorter = (TableRowSorter<DefaultTableModel>) teacherTable.getRowSorter();

        // Buttons
        int iconSize = 20;

        addButton = new JButton("Add");
        Icon addIcon = loadSVGIconButton("/icons/add.svg", iconSize);
        if (addIcon != null) addButton.setIcon(addIcon);

        editButton = new JButton("Edit");
        Icon editIcon = loadSVGIconButton("/icons/edit.svg", iconSize);
        if (editIcon != null) editButton.setIcon(editIcon);

        deleteButton = new JButton("Delete");
        Icon deleteIcon = loadSVGIconButton("/icons/delete.svg", iconSize);
        if (deleteIcon != null) deleteButton.setIcon(deleteIcon);

        refreshButton = new JButton("Refresh");
        Icon refreshIcon = loadSVGIconButton("/icons/refresh.svg", iconSize);
        if (refreshIcon != null) refreshButton.setIcon(refreshIcon);
        refreshButton.setToolTipText("Reload teacher data from storage");

        importButton = new JButton("Import");
        Icon importIcon = loadSVGIconButton("/icons/import.svg", iconSize); // Load SVG
        if (importIcon != null) importButton.setIcon(importIcon);
        importButton.setToolTipText("Import teachers data from an Excel file (.xlsx)");

        searchField = new JTextField(20);
        searchButton = new JButton("Search by Specialization");
    }

    private void setupLayout() {
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.add(importButton);
        actionPanel.add(refreshButton);
        actionPanel.add(addButton);
        actionPanel.add(editButton);
        actionPanel.add(deleteButton);

        topPanel.add(searchPanel, BorderLayout.WEST);
        topPanel.add(actionPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(teacherTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void setupActions() {
        addButton.addActionListener(e -> openTeacherDialog(null));

        editButton.addActionListener(e -> {
            int selectedRow = teacherTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = teacherTable.convertRowIndexToModel(selectedRow);
                int teacherId = (int) tableModel.getValueAt(modelRow, 0);
                Teacher teacherToEdit = controller.getTeacherById(teacherId);
                if (teacherToEdit != null) {
                    openTeacherDialog(teacherToEdit);
                } else {
                    UIUtils.showErrorMessage(this, "Error", "Could not retrieve teacher details for editing.");
                }
            } else {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select a teacher to edit.");
            }
        });

        deleteButton.addActionListener(e -> {
            int[] selectedViewRows = teacherTable.getSelectedRows();
            if (selectedViewRows.length == 0) {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select at least one teacher to delete.");
                return;
            }
            List<Integer> teacherIdsToDelete = new ArrayList<>();
            List<String> teacherNamesToDelete = new ArrayList<>();
            for (int viewRow : selectedViewRows) {
                int modelRow = teacherTable.convertRowIndexToModel(viewRow);
                teacherIdsToDelete.add((Integer) tableModel.getValueAt(modelRow, 0));
                teacherNamesToDelete.add((String) tableModel.getValueAt(modelRow, 1));
            }

            // Hiển thị thông báo xác nhận xóa nhiều
            String confirmationMessage = "Are you sure you want to delete the following "
                    + teacherIdsToDelete.size() + " teacher(s)?\n\n";
            int namesToShow = Math.min(teacherNamesToDelete.size(), 5);
            for (int i = 0; i < namesToShow; i++) {
                confirmationMessage += "- " + teacherNamesToDelete.get(i) + " (ID: " + teacherIdsToDelete.get(i) + ")\n";
            }
            if (teacherNamesToDelete.size() > namesToShow) {
                confirmationMessage += "... and " + (teacherNamesToDelete.size() - namesToShow) + " more.";
            }
            if (UIUtils.showConfirmDialog(this, "Confirm Deletion", confirmationMessage)) {
                System.out.println("Deletion confirmed for IDs: " + teacherIdsToDelete);
                if (controller != null) {
                    controller.deleteMultipleTeachers(teacherIdsToDelete);
                } else {
                    System.err.println("TeacherPanel Error: Controller is null, cannot delete.");
                }
            } else {
                System.out.println("Deletion cancelled by user.");
            }
        });
        searchButton.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());
        refreshButton.addActionListener(e -> {
            System.out.println("TeacherPanel: Refresh button clicked.");
            refreshTable();
            UIUtils.showInfoMessage(this,"Refreshed", "Teacher list updated."); // Thông báo (tùy chọn)
        });
        importButton.addActionListener(e -> {
            if (controller != null) {
                controller.importTeachersFromExcel();
            } else {
                UIUtils.showErrorMessage(this, "Error", "Teachers Controller not available.");
            }
        });
    }
    public void setAllButtonsEnabled(boolean enabled) {
        if (importButton != null) importButton.setEnabled(enabled);
        if (refreshButton != null) refreshButton.setEnabled(enabled);
        if (addButton != null) addButton.setEnabled(enabled);
        if (editButton != null) editButton.setEnabled(enabled);
        if (deleteButton != null) deleteButton.setEnabled(enabled);
    }

    private void performSearch() {
        if (controller == null) return;
        String searchText = searchField.getText();
        List<Teacher> teachers;
        if (searchText.trim().isEmpty()) {
            teachers = controller.getAllTeachers();
            sorter.setRowFilter(null);
        } else {
            teachers = controller.searchTeachersBySpecialization(searchText);
        }
        populateTable(teachers);
    }

    private void openTeacherDialog(Teacher teacher) {
        if (controller == null) {
            UIUtils.showErrorMessage(this, "Error", "Teacher Controller is not initialized.");
            return;
        }
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        TeacherDialog dialog = new TeacherDialog((Frame) parentWindow, controller, teacher);
        dialog.setVisible(true);
    }

    public void refreshTable() {
        if (controller == null) return;
        List<Teacher> teachers = controller.getAllTeachers();
        populateTable(teachers);
    }

    private void populateTable(List<Teacher> teachers) {
        tableModel.setRowCount(0);
        if (teachers != null) {
            for (Teacher teacher : teachers) {
                Vector<Object> row = new Vector<>();
                row.add(teacher.getTeacherId());
                row.add(teacher.getFullName());
                row.add(teacher.getSpecialization());
                row.add(teacher.getPhone());
                row.add(teacher.getEmail());
                row.add(DateUtils.formatDate(teacher.getDateOfBirth()));
                row.add(teacher.isActive());
                tableModel.addRow(row);
            }
        }
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
}