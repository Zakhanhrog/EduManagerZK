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
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

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
        addButton = new JButton("Add", UIUtils.createImageIcon("/icons/add.png", "Add"));
        editButton = new JButton("Edit", UIUtils.createImageIcon("/icons/edit.png", "Edit"));
        deleteButton = new JButton("Delete", UIUtils.createImageIcon("/icons/delete.png", "Delete"));
        refreshButton = new JButton("Refresh", UIUtils.createImageIcon("/icons/refresh.png", "Refresh"));  // <-- 2. KHỞI TẠO NÚT REFRESH
        refreshButton.setToolTipText("Reload teacher data from storage");
        importButton = new JButton("Import Excel", UIUtils.createImageIcon("/icons/import.png", "Refresh")); // <-- Khởi tạo
        importButton.setToolTipText("Import teachers data from an Excel file (.xlsx)");

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
        actionPanel.add(importButton);
        actionPanel.add(refreshButton);
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
            // --- THAY ĐỔI LOGIC XÓA ---
            int[] selectedViewRows = teacherTable.getSelectedRows(); // Lấy TẤT CẢ các hàng đang được chọn (chỉ số trên VIEW)

            if (selectedViewRows.length == 0) {
                // Không có hàng nào được chọn
                UIUtils.showWarningMessage(this, "Selection Required", "Please select at least one teacher to delete.");
                return; // Thoát khỏi hành động
            }

            // Chuyển đổi chỉ số View sang chỉ số Model và lấy IDs
            List<Integer> teacherIdsToDelete = new ArrayList<>();
            List<String> teacherNamesToDelete = new ArrayList<>();
            for (int viewRow : selectedViewRows) {
                int modelRow = teacherTable.convertRowIndexToModel(viewRow); // Quan trọng: Chuyển sang Model index
                teacherIdsToDelete.add((Integer) tableModel.getValueAt(modelRow, 0)); // Giả sử cột 0 là ID (Integer)
                teacherNamesToDelete.add((String) tableModel.getValueAt(modelRow, 1)); // Giả sử cột 1 là Tên (String)
            }

            // Hiển thị thông báo xác nhận xóa nhiều
            String confirmationMessage = "Are you sure you want to delete the following "
                    + teacherIdsToDelete.size() + " teacher(s)?\n\n";
            // Hiển thị tối đa 5-10 tên để xác nhận trực quan
            int namesToShow = Math.min(teacherNamesToDelete.size(), 5);
            for (int i = 0; i < namesToShow; i++) {
                confirmationMessage += "- " + teacherNamesToDelete.get(i) + " (ID: " + teacherIdsToDelete.get(i) + ")\n";
            }
            if (teacherNamesToDelete.size() > namesToShow) {
                confirmationMessage += "... and " + (teacherNamesToDelete.size() - namesToShow) + " more.";
            }

            if (UIUtils.showConfirmDialog(this, "Confirm Deletion", confirmationMessage)) {
                System.out.println("Deletion confirmed for IDs: " + teacherIdsToDelete);
                // Gọi phương thức xóa nhiều trong Controller
                if (controller != null) {
                    controller.deleteMultipleTeachers(teacherIdsToDelete); // Gọi hàm mới trong Controller
                    // Controller sẽ gọi refreshTable sau khi xóa xong
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
            refreshTable(); // Gọi lại chính phương thức refresh của panel này
            UIUtils.showInfoMessage(this,"Refreshed", "Teacher list updated."); // Thông báo (tùy chọn)
        });
        importButton.addActionListener(e -> {
            if (controller != null) {
                controller.importTeachersFromExcel(); // Gọi hàm mới trong controller
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