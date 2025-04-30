package com.eduzk.view.panels;

import com.eduzk.controller.TeacherController;
import com.eduzk.model.entities.Teacher;
import com.eduzk.utils.DateUtils;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.dialogs.TeacherDialog; // Import the dialog

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.Vector;

public class TeacherPanel extends JPanel {

    private TeacherController controller;
    private JTable teacherTable;
    private DefaultTableModel tableModel;
    private JButton addButton, editButton, deleteButton, searchButton;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;

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
        teacherTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        teacherTable.setAutoCreateRowSorter(true);
        sorter = (TableRowSorter<DefaultTableModel>) teacherTable.getRowSorter();

        // Buttons
        addButton = new JButton("Add", UIUtils.createImageIcon("/icons/add.png", "Add"));
        editButton = new JButton("Edit", UIUtils.createImageIcon("/icons/edit.png", "Edit"));
        deleteButton = new JButton("Delete", UIUtils.createImageIcon("/icons/delete.png", "Delete"));

        // Search Components
        searchField = new JTextField(20);
        searchButton = new JButton("Search by Specialization");
    }

    private void setupLayout() {
        // Top Panel (Search and Actions)
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.add(addButton);
        actionPanel.add(editButton);
        actionPanel.add(deleteButton);

        topPanel.add(searchPanel, BorderLayout.WEST);
        topPanel.add(actionPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center Panel (Table)
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
            int selectedRow = teacherTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = teacherTable.convertRowIndexToModel(selectedRow);
                int teacherId = (int) tableModel.getValueAt(modelRow, 0);
                String teacherName = (String) tableModel.getValueAt(modelRow, 1);

                if (UIUtils.showConfirmDialog(this, "Confirm Deletion", "Are you sure you want to delete teacher '" + teacherName + "' (ID: " + teacherId + ")?")) {
                    if (controller != null) {
                        controller.deleteTeacher(teacherId);
                    }
                }
            } else {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select a teacher to delete.");
            }
        });

        searchButton.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());
    }

    private void performSearch() {
        if (controller == null) return;
        String searchText = searchField.getText();
        List<Teacher> teachers;
        if (searchText.trim().isEmpty()) {
            teachers = controller.getAllTeachers();
            sorter.setRowFilter(null); // Clear filter when search is empty
        } else {
            // Search via controller
            teachers = controller.searchTeachersBySpecialization(searchText);
            // If searching via DAO, RowFilter is not needed here.
            // If filtering the full list in memory:
            // RowFilter<DefaultTableModel, Object> rf = RowFilter.regexFilter("(?i)" + searchText, 2); // Column 2 = Specialization
            // sorter.setRowFilter(rf);
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
        // Refresh is handled by controller after successful save in dialog
    }

    public void refreshTable() {
        if (controller == null) return;
        List<Teacher> teachers = controller.getAllTeachers();
        populateTable(teachers);
    }

    private void populateTable(List<Teacher> teachers) {
        tableModel.setRowCount(0); // Clear existing data
        if (teachers != null) {
            for (Teacher teacher : teachers) {
                Vector<Object> row = new Vector<>();
                row.add(teacher.getTeacherId());
                row.add(teacher.getFullName());
                row.add(teacher.getSpecialization());
                row.add(teacher.getPhone());
                row.add(teacher.getEmail());
                row.add(DateUtils.formatDate(teacher.getDateOfBirth()));
                row.add(teacher.isActive()); // Add boolean value for checkbox rendering
                tableModel.addRow(row);
            }
        }
    }

    public void setAdminControlsEnabled(boolean isAdmin) {
        addButton.setVisible(isAdmin); // Hoặc setEnabled(isAdmin)
        editButton.setVisible(isAdmin);
        deleteButton.setVisible(isAdmin);
        // Các nút khác (Search) có thể luôn hiển thị/enabled
        // searchButton.setEnabled(true);
        // searchField.setEnabled(true);
    }
}