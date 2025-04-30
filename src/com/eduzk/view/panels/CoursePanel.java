package com.eduzk.view.panels;

import com.eduzk.controller.CourseController;
import com.eduzk.model.entities.Course;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.dialogs.CourseDialog; // Import the dialog

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.Vector;

public class CoursePanel extends JPanel {

    private CourseController controller;
    private JTable courseTable;
    private DefaultTableModel tableModel;
    private JButton addButton, editButton, deleteButton, searchButton;
    private JButton refreshButton;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;


    public CoursePanel(CourseController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
        setupLayout();
        setupActions();
    }

    private void setupLayout() {
        // Top Panel (Search and Actions)
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.add(refreshButton);
        actionPanel.add(addButton);
        actionPanel.add(editButton);
        actionPanel.add(deleteButton);

        topPanel.add(searchPanel, BorderLayout.WEST);
        topPanel.add(actionPanel, BorderLayout.EAST);

        // Thêm topPanel vào panel chính (this) ở phía Bắc
        add(topPanel, BorderLayout.NORTH);

        // Center Panel (Table)
        // Đặt courseTable vào trong JScrollPane
        JScrollPane scrollPane = new JScrollPane(courseTable);
        // Thêm scrollPane vào panel chính (this) ở giữa
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setController(CourseController controller) {
        this.controller = controller;
        refreshTable(); // Initial load
    }

    private void initComponents() {
        // Table Model
        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Code", "Name", "Level", "Credits", "Description"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        courseTable = new JTable(tableModel);
        courseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        courseTable.setAutoCreateRowSorter(true);
        sorter = (TableRowSorter<DefaultTableModel>) courseTable.getRowSorter();

        // Adjust column widths (optional, but often helpful)
        TableColumn idCol = courseTable.getColumnModel().getColumn(0);
        idCol.setPreferredWidth(40);
        idCol.setMaxWidth(60);
        TableColumn codeCol = courseTable.getColumnModel().getColumn(1);
        codeCol.setPreferredWidth(80);
        TableColumn nameCol = courseTable.getColumnModel().getColumn(2);
        nameCol.setPreferredWidth(250);
        TableColumn levelCol = courseTable.getColumnModel().getColumn(3);
        levelCol.setPreferredWidth(100);
        TableColumn creditsCol = courseTable.getColumnModel().getColumn(4);
        creditsCol.setPreferredWidth(60);
        TableColumn descCol = courseTable.getColumnModel().getColumn(5);
        descCol.setPreferredWidth(300);


        // Buttons
        addButton = new JButton("Add", UIUtils.createImageIcon("/icons/add.png", "Add"));
        editButton = new JButton("Edit", UIUtils.createImageIcon("/icons/edit.png", "Edit"));
        deleteButton = new JButton("Delete", UIUtils.createImageIcon("/icons/delete.png", "Delete"));
        refreshButton = new JButton("Refresh"); // <-- 2. KHỞI TẠO NÚT REFRESH
        refreshButton.setToolTipText("Reload course data from storage"); // Thêm gợi ý


        // Search Components
        searchField = new JTextField(20);
        searchButton = new JButton("Search by Name");
    }

    // ... (Phần initComponents và setupLayout đã có ở trên)

    private void setupActions() {
        addButton.addActionListener(e -> openCourseDialog(null));

        editButton.addActionListener(e -> {
            int selectedRow = courseTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = courseTable.convertRowIndexToModel(selectedRow);
                int courseId = (int) tableModel.getValueAt(modelRow, 0); // ID column
                Course courseToEdit = controller.getCourseById(courseId);
                if (courseToEdit != null) {
                    openCourseDialog(courseToEdit);
                } else {
                    UIUtils.showErrorMessage(this, "Error", "Could not retrieve course details for editing.");
                }
            } else {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select a course to edit.");
            }
        });

        deleteButton.addActionListener(e -> {
            int selectedRow = courseTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = courseTable.convertRowIndexToModel(selectedRow);
                int courseId = (int) tableModel.getValueAt(modelRow, 0);
                String courseName = (String) tableModel.getValueAt(modelRow, 2); // Name column

                if (UIUtils.showConfirmDialog(this, "Confirm Deletion", "Are you sure you want to delete course '" + courseName + "' (ID: " + courseId + ")?")) {
                    if (controller != null) {
                        controller.deleteCourse(courseId);
                    }
                }
            } else {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select a course to delete.");
            }
        });

        searchButton.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());
        refreshButton.addActionListener(e -> {
            System.out.println("CoursePanel: Refresh button clicked.");
            refreshTable(); // Gọi lại chính phương thức refresh của panel này
            UIUtils.showInfoMessage(this,"Refreshed", "Course list updated."); // Thông báo (tùy chọn)
        });
    }

    private void performSearch() {
        if (controller == null) return;
        String searchText = searchField.getText();
        List<Course> courses;
        if (searchText.trim().isEmpty()) {
            courses = controller.getAllCourses();
            sorter.setRowFilter(null); // Clear filter
        } else {
            // Search via controller
            courses = controller.searchCoursesByName(searchText);
            // Optional: Use RowFilter if filtering in memory
            // RowFilter<DefaultTableModel, Object> rf = RowFilter.regexFilter("(?i)" + searchText, 2); // Column 2 = Name
            // sorter.setRowFilter(rf);
        }
        populateTable(courses);
    }


    private void openCourseDialog(Course course) {
        if (controller == null) {
            UIUtils.showErrorMessage(this, "Error", "Course Controller is not initialized.");
            return;
        }
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        CourseDialog dialog = new CourseDialog((Frame) parentWindow, controller, course);
        dialog.setVisible(true);
    }


    public void refreshTable() {
        if (controller == null) return;
        List<Course> courses = controller.getAllCourses();
        populateTable(courses);
    }

    private void populateTable(List<Course> courses) {
        tableModel.setRowCount(0); // Clear existing data
        if (courses != null) {
            for (Course course : courses) {
                Vector<Object> row = new Vector<>();
                row.add(course.getCourseId());
                row.add(course.getCourseCode());
                row.add(course.getCourseName());
                row.add(course.getLevel());
                row.add(course.getCredits()); // Assuming credits is int
                row.add(course.getDescription());
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
} // End of CoursePanel class