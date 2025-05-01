package com.eduzk.view.panels;

import com.eduzk.controller.StudentController;
import com.eduzk.model.entities.Student;
import com.eduzk.utils.DateUtils;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.dialogs.StudentDialog; // Import the dialog
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Arrays;

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
        setLayout(new BorderLayout(10, 10)); // Add gaps
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding
        initComponents();
        setupLayout();
        setupActions();
    }

    // Method to set the controller after instantiation (used by MainView)
    public void setController(StudentController controller) {
        this.controller = controller;
        // Initial data load when controller is set
        refreshTable();
    }

    private void initComponents() {
        // Table Model
        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Full Name", "DOB", "Gender", "Phone", "Email", "Parent", "Password"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table cells non-editable by default
            }
        };
        studentTable = new JTable(tableModel);
        studentTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        studentTable.setAutoCreateRowSorter(true); // Enable basic sorting
        sorter = (TableRowSorter<DefaultTableModel>) studentTable.getRowSorter();


        // Buttons
        addButton = new JButton("Add", UIUtils.createImageIcon("/icons/add.png", "Add"));
        editButton = new JButton("Edit", UIUtils.createImageIcon("/icons/edit.png", "Edit"));
        deleteButton = new JButton("Delete", UIUtils.createImageIcon("/icons/delete.png", "Delete"));
        refreshButton = new JButton("Refresh", UIUtils.createImageIcon("/icons/refresh.png", "Refresh"));  // <-- 2. KHỞI TẠO NÚT REFRESH
        refreshButton.setToolTipText("Reload student data from storage");
        importButton = new JButton("Import", UIUtils.createImageIcon("/icons/import.png", "Import"));
        importButton.setToolTipText("Import student data from an Excel file (.xlsx)");

        // Search Components
        searchField = new JTextField(20);
        searchButton = new JButton("Search by Name");

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

        actionPanel.add(addButton);
        actionPanel.add(editButton);
        actionPanel.add(deleteButton);

        topPanel.add(searchPanel, BorderLayout.WEST);
        topPanel.add(actionPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center Panel (Table)
        JScrollPane scrollPane = new JScrollPane(studentTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void setupActions() {
        addButton.addActionListener(e -> openStudentDialog(null)); // Pass null for add mode

        editButton.addActionListener(e -> {
            int selectedRow = studentTable.getSelectedRow();
            if (selectedRow >= 0) {
                // Convert view row index to model row index in case of sorting/filtering
                int modelRow = studentTable.convertRowIndexToModel(selectedRow);
                int studentId = (int) tableModel.getValueAt(modelRow, 0); // Assuming ID is column 0
                Student studentToEdit = controller.getStudentById(studentId);
                if (studentToEdit != null) {
                    openStudentDialog(studentToEdit); // Pass student object for edit mode
                } else {
                    UIUtils.showErrorMessage(this, "Error", "Could not retrieve student details for editing.");
                }
            } else {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select a student to edit.");
            }
        });

        deleteButton.addActionListener(e -> {
            // Lấy tất cả các hàng đang được chọn trên VIEW
            int[] selectedViewRows = studentTable.getSelectedRows();

            if (selectedViewRows.length == 0) { // Không có hàng nào được chọn
                UIUtils.showWarningMessage(this, "Selection Required", "Please select one or more students to delete.");
                return;
            }

            // Chuyển đổi chỉ số hàng từ VIEW sang MODEL (quan trọng khi có sắp xếp/lọc)
            // và lấy danh sách ID cần xóa
            List<Integer> idsToDelete = new ArrayList<>();
            List<String> namesToDelete = new ArrayList<>(); // Để hiển thị trong thông báo xác nhận
            for (int viewRow : selectedViewRows) {
                int modelRow = studentTable.convertRowIndexToModel(viewRow);
                if (modelRow >= 0) { // Đảm bảo hàng vẫn tồn tại trong model
                    idsToDelete.add((Integer) tableModel.getValueAt(modelRow, 0)); // Giả sử cột 0 là ID (Integer)
                    namesToDelete.add((String) tableModel.getValueAt(modelRow, 1)); // Giả sử cột 1 là Name (String)
                }
            }

            if (idsToDelete.isEmpty()) {
                // Có thể xảy ra nếu các hàng được chọn đã bị xóa bởi thao tác khác
                UIUtils.showWarningMessage(this, "Error", "Could not find selected students in the data model.");
                return;
            }

            // Tạo thông điệp xác nhận
            String confirmationMessage;
            if (idsToDelete.size() == 1) {
                confirmationMessage = "Are you sure you want to delete student '" + namesToDelete.get(0) + "' (ID: " + idsToDelete.get(0) + ")?";
            } else {
                confirmationMessage = "Are you sure you want to delete these " + idsToDelete.size() + " students?\n"
                        + "(IDs: " + idsToDelete.toString() + ")"; // Hiển thị danh sách ID
            }

            // Hiển thị hộp thoại xác nhận
            if (UIUtils.showConfirmDialog(this, "Confirm Deletion", confirmationMessage)) {
                if (controller != null) {
                    // Gọi phương thức xóa hàng loạt mới trong Controller
                    System.out.println("Requesting deletion for student IDs: " + idsToDelete);
                    controller.deleteStudents(idsToDelete); // <-- Gọi hàm xóa mới
                    // Refresh sẽ được gọi trong controller nếu xóa thành công
                } else {
                    UIUtils.showErrorMessage(this, "Error", "Student Controller not available.");
                }
            }
        });

        searchButton.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch()); // Allow Enter key in search field
        // --- 4. THÊM ACTIONLISTENER CHO REFRESH BUTTON ---
        refreshButton.addActionListener(e -> {
            System.out.println("StudentPanel: Refresh button clicked.");
            refreshTable(); // Gọi lại chính phương thức refresh của panel này
            UIUtils.showInfoMessage(this,"Refreshed", "Student list updated."); // Thông báo (tùy chọn)
        });

        importButton.addActionListener(e -> {
            if (controller != null) {
                controller.importStudentsFromExcel(); // Gọi hàm mới trong controller
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
            // If search is empty, load all
            students = controller.getAllStudents();
            // Clear any existing filter on the sorter
            sorter.setRowFilter(null);
        } else {
            // Perform search via controller (which calls DAO)
            students = controller.searchStudentsByName(searchText);
            // Optionally apply a RowFilter for more dynamic filtering (if not searching via DAO)
            // RowFilter<DefaultTableModel, Object> rf = RowFilter.regexFilter("(?i)" + searchText, 1); // Column 1 = Full Name
            // sorter.setRowFilter(rf);
        }
        populateTable(students); // Update table with search results

    }

    private void openStudentDialog(Student student) {
        if (controller == null) {
            UIUtils.showErrorMessage(this, "Error", "Student Controller is not initialized.");
            return;
        }
        // Create and show the dialog
        // The dialog needs a reference to the controller to perform add/update
        // Pass the parent window (this panel's top-level window)
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        StudentDialog dialog = new StudentDialog((Frame) parentWindow, controller, student); // Pass null for add, student obj for edit
        dialog.setVisible(true);

        // The dialog should call controller.add/update, and controller calls refreshTable here
    }

    // Method called by the controller to refresh the table data
    public void refreshTable() {
        if (controller == null) return; // Do nothing if controller isn't set yet
        List<Student> students = controller.getAllStudents();
        populateTable(students);
        // Clear search field after full refresh? Optional.
        // searchField.setText("");
        // sorter.setRowFilter(null);
    }

    // Helper method to populate the table model from a list of students
    private void populateTable(List<Student> students) {
        tableModel.setRowCount(0); // Clear existing data
        if (students != null) {
            for (Student student : students) {
                Vector<Object> row = new Vector<>();
                row.add(student.getStudentId());
                row.add(student.getFullName());
                row.add(DateUtils.formatDate(student.getDateOfBirth())); // Use DateUtils
                row.add(student.getGender());
                row.add(student.getPhone());
                row.add(student.getEmail());
                row.add(student.getParentName());
                String password = controller.getPasswordForStudent(student.getStudentId()); // Cần tạo hàm này trong Controller
                row.add(password != null ? password : "N/A"); // Hiển thị N/A nếu chưa có tài khoản
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

    public void setAllButtonsEnabled(boolean enabled) {
        if (importButton != null) importButton.setEnabled(enabled);
        if (refreshButton != null) refreshButton.setEnabled(enabled);
        if (addButton != null) addButton.setEnabled(enabled);
        if (editButton != null) editButton.setEnabled(enabled);
        if (deleteButton != null) deleteButton.setEnabled(enabled);
        // Có thể không cần vô hiệu hóa nút Search
        // if (searchButton != null) searchButton.setEnabled(enabled);
        // if (searchField != null) searchField.setEnabled(enabled);
    }
}