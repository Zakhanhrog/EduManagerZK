package com.eduzk.view.panels;

import com.eduzk.controller.CourseController;
import com.eduzk.model.entities.Course;
import com.eduzk.model.entities.Role;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.dialogs.CourseDialog;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.Vector;
import javax.swing.Icon;
import java.net.URL;
import com.formdev.flatlaf.extras.FlatSVGIcon;

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

        add(topPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(courseTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setController(CourseController controller) {
        this.controller = controller;
        refreshTable();
    }

    private void initComponents() {
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
        int iconSize = 20;
        addButton = new JButton("Add Course");
        Icon addIcon = loadSVGIconButton("/icons/add.svg", iconSize);
        if (addIcon != null) addButton.setIcon(addIcon);

        editButton = new JButton("Edit Course");
        Icon editIcon = loadSVGIconButton("/icons/edit.svg", iconSize);
        if (editIcon != null) editButton.setIcon(editIcon);

        deleteButton = new JButton("Delete Course");
        Icon deleteIcon = loadSVGIconButton("/icons/delete.svg", iconSize);
        if (deleteIcon != null) deleteButton.setIcon(deleteIcon);

        refreshButton = new JButton("Refresh");
        Icon refreshIcon = loadSVGIconButton("/icons/refresh.svg", iconSize);
        if (refreshIcon != null) refreshButton.setIcon(refreshIcon);
        refreshButton.setToolTipText("Reload class data from storage");

        // Search Components
        searchField = new JTextField(20);
        searchButton = new JButton("Search by Name");
    }

    private void setupActions() {
        addButton.addActionListener(e -> openCourseDialog(null));
        editButton.addActionListener(e -> {
            int selectedRow = courseTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = courseTable.convertRowIndexToModel(selectedRow);
                int courseId = (int) tableModel.getValueAt(modelRow, 0);
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
                String courseName = (String) tableModel.getValueAt(modelRow, 2);

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
            refreshTable();
            UIUtils.showInfoMessage(this,"Refreshed", "Course list updated.");
        });
    }

    private void performSearch() {
        if (controller == null) return;
        String searchText = searchField.getText();
        List<Course> courses;
        if (searchText.trim().isEmpty()) {
            courses = controller.getAllCourses();
            sorter.setRowFilter(null);
        } else {
            courses = controller.searchCoursesByName(searchText);
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
        tableModel.setRowCount(0);
        if (courses != null) {
            for (Course course : courses) {
                Vector<Object> row = new Vector<>();
                row.add(course.getCourseId());
                row.add(course.getCourseCode());
                row.add(course.getCourseName());
                row.add(course.getLevel());
                row.add(course.getCredits());
                row.add(course.getDescription());
                tableModel.addRow(row);
            }
        }
    }

    public void configureControlsForRole(Role userRole) {
        boolean canModify = (userRole == Role.ADMIN);
        if (addButton != null) addButton.setVisible(canModify);
        if (editButton != null) editButton.setVisible(canModify);
        if (deleteButton != null) deleteButton.setVisible(canModify);
        if (searchButton != null) searchButton.setVisible(true);
        if (searchField != null) searchField.setVisible(true);
        if (refreshButton != null) refreshButton.setVisible(true);

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