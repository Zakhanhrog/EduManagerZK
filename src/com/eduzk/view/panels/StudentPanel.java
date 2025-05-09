package com.eduzk.view.panels;

import com.eduzk.controller.StudentController;
import com.eduzk.model.entities.Role;
import com.eduzk.model.entities.Student;
import com.eduzk.utils.DateUtils;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.dialogs.StudentDialog;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.net.URL;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;
import javax.swing.Icon;

public class StudentPanel extends JPanel {

    private StudentController controller;
    private JTable studentTable;
    private DefaultTableModel tableModel;
    private JButton addButton, editButton, deleteButton, searchButton;
    private JButton refreshButton;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;
    private JButton importButton;


    public StudentPanel(StudentController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
        setupLayout();
        setupActions();
    }

    public void setController(StudentController controller) {
        this.controller = controller;
        refreshTable();
    }

    private void initComponents() {
        // Table Model
        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Full Name", "DOB", "Gender", "Phone", "Email", "Parent"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        studentTable = new JTable(tableModel);
        studentTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        studentTable.setAutoCreateRowSorter(true);
        sorter = (TableRowSorter<DefaultTableModel>) studentTable.getRowSorter();

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
        refreshButton.setToolTipText("Reload student data from storage");

        importButton = new JButton("Import");
        Icon importIcon = loadSVGIconButton("/icons/import.svg", iconSize);
        if (importIcon != null) importButton.setIcon(importIcon);
        importButton.setToolTipText("Import student data from an Excel file (.xlsx)");

        // Search Components
        searchField = new JTextField(20);
        searchButton = new JButton("Search by Name");

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

        actionPanel.add(addButton);
        actionPanel.add(editButton);
        actionPanel.add(deleteButton);

        topPanel.add(searchPanel, BorderLayout.WEST);
        topPanel.add(actionPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(studentTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void setupActions() {
        addButton.addActionListener(e -> openStudentDialog(null));

        editButton.addActionListener(e -> {
            int selectedRow = studentTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = studentTable.convertRowIndexToModel(selectedRow);
                int studentId = (int) tableModel.getValueAt(modelRow, 0);
                Student studentToEdit = controller.getStudentById(studentId);
                if (studentToEdit != null) {
                    openStudentDialog(studentToEdit);
                } else {
                    UIUtils.showErrorMessage(this, "Error", "Could not retrieve student details for editing.");
                }
            } else {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select a student to edit.");
            }
        });

        deleteButton.addActionListener(e -> {
            int[] selectedViewRows = studentTable.getSelectedRows();

            if (selectedViewRows.length == 0) {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select one or more students to delete.");
                return;
            }

            List<Integer> idsToDelete = new ArrayList<>();
            List<String> namesToDelete = new ArrayList<>();
            for (int viewRow : selectedViewRows) {
                int modelRow = studentTable.convertRowIndexToModel(viewRow);
                if (modelRow >= 0) {
                    idsToDelete.add((Integer) tableModel.getValueAt(modelRow, 0));
                    namesToDelete.add((String) tableModel.getValueAt(modelRow, 1));
                }
            }

            if (idsToDelete.isEmpty()) {
                UIUtils.showWarningMessage(this, "Error", "Could not find selected students in the data model.");
                return;
            }
            String confirmationMessage;
            if (idsToDelete.size() == 1) {
                confirmationMessage = "Are you sure you want to delete student '" + namesToDelete.get(0) + "' (ID: " + idsToDelete.get(0) + ")?";
            } else {
                confirmationMessage = "Are you sure you want to delete these " + idsToDelete.size() + " students?\n"
                        + "(IDs: " + idsToDelete.toString() + ")";
            }
            if (UIUtils.showConfirmDialog(this, "Confirm Deletion", confirmationMessage)) {
                if (controller != null) {
                    System.out.println("Requesting deletion for student IDs: " + idsToDelete);
                    controller.deleteStudents(idsToDelete);
                } else {
                    UIUtils.showErrorMessage(this, "Error", "Student Controller not available.");
                }
            }
        });

        searchButton.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());
        // --- 4. THÊM ACTIONLISTENER CHO REFRESH BUTTON ---
        refreshButton.addActionListener(e -> {
            System.out.println("StudentPanel: Refresh button clicked.");
            refreshTable();
            UIUtils.showInfoMessage(this,"Refreshed", "Student list updated.");
        });

        importButton.addActionListener(e -> {
            if (controller != null) {
                controller.importStudentsFromExcel();
            } else {
                UIUtils.showErrorMessage(this, "Error", "Student Controller not available.");
            }
        });
    }

    private void performSearch() {
        if (controller == null) return;
        String searchText = searchField.getText();
        List<Student> students;
        if (searchText.trim().isEmpty()) {
            students = controller.getAllStudents();
            sorter.setRowFilter(null);
        } else {
            students = controller.searchStudentsByName(searchText);
        }
        populateTable(students);
    }

    private void openStudentDialog(Student student) {
        if (controller == null) {
            UIUtils.showErrorMessage(this, "Error", "Student Controller is not initialized.");
            return;
        }
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        StudentDialog dialog = new StudentDialog((Frame) parentWindow, controller, student);
        dialog.setVisible(true);
    }

    public void refreshTable() {
        if (controller == null) return;
        List<Student> students = controller.getAllStudents();
        populateTable(students);
    }

    private void populateTable(List<Student> students) {
        tableModel.setRowCount(0);
        if (students != null) {
            for (Student student : students) {
                Vector<Object> row = new Vector<>();
                row.add(student.getStudentId());
                row.add(student.getFullName());
                row.add(DateUtils.formatDate(student.getDateOfBirth()));
                row.add(student.getGender());
                row.add(student.getPhone());
                row.add(student.getEmail());
                row.add(student.getParentName());
                tableModel.addRow(row);
            }
        }
    }
    public void setAdminControlsEnabled(boolean isAdmin) {
        addButton.setVisible(isAdmin);
        editButton.setVisible(isAdmin);
        deleteButton.setVisible(isAdmin);
    }

    public void setAllButtonsEnabled(boolean enabled) {
        if (importButton != null) importButton.setEnabled(enabled);
        if (refreshButton != null) refreshButton.setEnabled(enabled);
        if (addButton != null) addButton.setEnabled(enabled);
        if (editButton != null) editButton.setEnabled(enabled);
        if (deleteButton != null) deleteButton.setEnabled(enabled);
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

    public void configureControlsForRole(Role userRole) {
        // Admin HOẶC Teacher được phép thao tác
        boolean canModify = (userRole == Role.ADMIN || userRole == Role.TEACHER);
        boolean canImport = (userRole == Role.ADMIN || userRole == Role.TEACHER);

        // Ẩn/hiện các nút thao tác
        if (addButton != null) addButton.setVisible(canModify);
        if (editButton != null) editButton.setVisible(canModify);
        if (deleteButton != null) deleteButton.setVisible(canModify);
        if (importButton != null) importButton.setVisible(canModify);

        // Các nút khác như Search, Refresh có thể luôn hiển thị
        if (importButton != null) importButton.setVisible(canImport);
        if (searchButton != null) searchButton.setVisible(true);
        if (searchField != null) searchField.setVisible(true);
        if (refreshButton != null) refreshButton.setVisible(true);
    }
}