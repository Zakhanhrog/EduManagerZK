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

public class StudentPanel extends JPanel {

    private StudentController controller;
    private JTable studentTable;
    private DefaultTableModel tableModel;
    private JButton addButton, editButton, deleteButton, searchButton;
    private JButton refreshButton;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;


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
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Allow only one row selection
        studentTable.setAutoCreateRowSorter(true); // Enable basic sorting
        sorter = (TableRowSorter<DefaultTableModel>) studentTable.getRowSorter();


        // Buttons
        addButton = new JButton("Add", UIUtils.createImageIcon("/icons/add.png", "Add"));
        editButton = new JButton("Edit", UIUtils.createImageIcon("/icons/edit.png", "Edit"));
        deleteButton = new JButton("Delete", UIUtils.createImageIcon("/icons/delete.png", "Delete"));
        refreshButton = new JButton("Refresh", UIUtils.createImageIcon("/icons/refresh.png", "Refresh"));  // <-- 2. KHỞI TẠO NÚT REFRESH
        refreshButton.setToolTipText("Reload student data from storage");

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
        actionPanel.add(refreshButton); // Thêm vào trước hoặc sau tùy ý
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
            int selectedRow = studentTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = studentTable.convertRowIndexToModel(selectedRow);
                int studentId = (int) tableModel.getValueAt(modelRow, 0);
                String studentName = (String) tableModel.getValueAt(modelRow, 1); // Get name for confirmation

                if (UIUtils.showConfirmDialog(this, "Confirm Deletion", "Are you sure you want to delete student '" + studentName + "' (ID: " + studentId + ")?")) {
                    if (controller != null) {
                        controller.deleteStudent(studentId);
                        // refreshTable() will be called by the controller on success
                    }
                }
            } else {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select a student to delete.");
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
}