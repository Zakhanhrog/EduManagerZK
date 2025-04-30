package com.eduzk.view.panels;

import com.eduzk.controller.EduClassController;
import com.eduzk.model.entities.Course;
import com.eduzk.model.entities.EduClass;
import com.eduzk.model.entities.Student;
import com.eduzk.model.entities.Teacher;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.dialogs.ClassDialog; // Dialog for add/edit class
// Consider a separate dialog for student selection/enrollment

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.Vector;

public class ClassPanel extends JPanel {

    private EduClassController controller;
    private JTable classTable;
    private DefaultTableModel classTableModel;
    private JTable enrolledStudentTable;
    private DefaultTableModel studentTableModel;
    private JButton addClassButton, editClassButton, deleteClassButton;
    private JButton enrollStudentButton, unenrollStudentButton;
    private JSplitPane splitPane;
    private JLabel selectedClassLabel; // Show details of selected class
    private TableRowSorter<DefaultTableModel> classSorter;
    private JButton refreshButton;

    public ClassPanel(EduClassController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
        setupLayout();
        setupActions();
    }

    public void setController(EduClassController controller) {
        this.controller = controller;
        refreshTable(); // Initial load of classes
    }

    private void initComponents() {
        // --- Class Table ---
        classTableModel = new DefaultTableModel(
                new Object[]{"ID", "Class Name", "Course", "Teacher", "Year", "Semester", "Capacity", "Enrolled"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 6 || columnIndex == 7) return Integer.class; // Capacity, Enrolled
                return super.getColumnClass(columnIndex);
            }
        };
        classTable = new JTable(classTableModel);
        classTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        classTable.setAutoCreateRowSorter(true);
        classSorter = (TableRowSorter<DefaultTableModel>) classTable.getRowSorter();


        // --- Enrolled Student Table ---
        studentTableModel = new DefaultTableModel(new Object[]{"Student ID", "Student Name"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        enrolledStudentTable = new JTable(studentTableModel);
        enrolledStudentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // --- Buttons ---
        addClassButton = new JButton("Add Class", UIUtils.createImageIcon("/icons/add.png", "Add Class"));
        editClassButton = new JButton("Edit Class", UIUtils.createImageIcon("/icons/edit.png", "Edit Class"));
        deleteClassButton = new JButton("Delete Class", UIUtils.createImageIcon("/icons/delete.png", "Delete Class"));
        refreshButton = new JButton("Refresh"); // <-- 2. KHỞI TẠO NÚT REFRESH
        refreshButton.setToolTipText("Reload class data from storage"); // Thêm gợi ý
        enrollStudentButton = new JButton("Enroll Student");
        unenrollStudentButton = new JButton("Unenroll Student");
        enrollStudentButton.setEnabled(false); // Disabled until a class is selected
        unenrollStudentButton.setEnabled(false);

        selectedClassLabel = new JLabel("Select a class to see enrolled students.");
        selectedClassLabel.setFont(selectedClassLabel.getFont().deriveFont(Font.ITALIC));
        selectedClassLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
    }

    private void setupLayout() {
        // --- Top Panel for Class Actions ---
        JPanel classActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        classActionPanel.add(addClassButton);
        classActionPanel.add(editClassButton);
        classActionPanel.add(deleteClassButton);
        classActionPanel.add(refreshButton);

        // --- Left Panel (Class Table) ---
        JPanel leftPanel = new JPanel(new BorderLayout(0, 5));
        leftPanel.add(classActionPanel, BorderLayout.NORTH);
        JScrollPane classScrollPane = new JScrollPane(classTable);
        leftPanel.add(classScrollPane, BorderLayout.CENTER);
        leftPanel.setMinimumSize(new Dimension(400, 200)); // Ensure left panel is reasonably sized

        // --- Right Panel (Student List and Actions) ---
        JPanel studentActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        studentActionPanel.add(enrollStudentButton);
        studentActionPanel.add(unenrollStudentButton);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 5));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Enrolled Students"));
        rightPanel.add(selectedClassLabel, BorderLayout.NORTH); // Show which class is selected
        JScrollPane studentScrollPane = new JScrollPane(enrolledStudentTable);
        rightPanel.add(studentScrollPane, BorderLayout.CENTER);
        rightPanel.add(studentActionPanel, BorderLayout.SOUTH);
        rightPanel.setMinimumSize(new Dimension(300, 200)); // Ensure right panel is reasonably sized

        // --- Split Pane ---
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(600); // Initial divider position
        splitPane.setResizeWeight(0.6); // Give more weight to the left panel on resize

        add(splitPane, BorderLayout.CENTER);
    }

    private void setupActions() {
        // --- Class Actions ---
        addClassButton.addActionListener(e -> openClassDialog(null));

        editClassButton.addActionListener(e -> {
            int selectedRow = classTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = classTable.convertRowIndexToModel(selectedRow);
                int classId = (int) classTableModel.getValueAt(modelRow, 0);
                EduClass classToEdit = controller.getEduClassById(classId);
                if (classToEdit != null) {
                    openClassDialog(classToEdit);
                } else {
                    UIUtils.showErrorMessage(this, "Error", "Could not retrieve class details for editing.");
                }
            } else {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select a class to edit.");
            }
        });

        deleteClassButton.addActionListener(e -> {
            int selectedRow = classTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = classTable.convertRowIndexToModel(selectedRow);
                int classId = (int) classTableModel.getValueAt(modelRow, 0);
                String className = (String) classTableModel.getValueAt(modelRow, 1);

                if (UIUtils.showConfirmDialog(this, "Confirm Deletion", "Are you sure you want to delete class '" + className + "' (ID: " + classId + ")?")) {
                    if (controller != null) {
                        controller.deleteEduClass(classId);
                    }
                }
            } else {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select a class to delete.");
            }
        });

        // --- Student Enrollment Actions ---
        enrollStudentButton.addActionListener(e -> enrollStudentAction());
        unenrollStudentButton.addActionListener(e -> unenrollStudentAction());
        refreshButton.addActionListener(e -> {
            System.out.println("ClassPanel: Refresh button clicked.");
            refreshTable(); // Gọi lại chính phương thức refresh của panel này
            UIUtils.showInfoMessage(this,"Refreshed", "Class list updated."); // Thông báo (tùy chọn)
        });


        // --- Table Selection Listener ---
        classTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) { // Prevent double events
                    int selectedRow = classTable.getSelectedRow();
                    boolean classSelected = (selectedRow >= 0);
                    enrollStudentButton.setEnabled(classSelected);
                    unenrollStudentButton.setEnabled(classSelected);

                    if (classSelected) {
                        int modelRow = classTable.convertRowIndexToModel(selectedRow);
                        int classId = (int) classTableModel.getValueAt(modelRow, 0);
                        String className = (String) classTableModel.getValueAt(modelRow, 1);
                        selectedClassLabel.setText("Students in: " + className);
                        selectedClassLabel.setFont(selectedClassLabel.getFont().deriveFont(Font.BOLD));
                        refreshStudentListForSelectedClass(); // Load students for the selected class
                    } else {
                        selectedClassLabel.setText("Select a class to see enrolled students.");
                        selectedClassLabel.setFont(selectedClassLabel.getFont().deriveFont(Font.ITALIC));
                        studentTableModel.setRowCount(0); // Clear student table if no class is selected
                    }
                }
            }
        });
    }

    private void openClassDialog(EduClass eduClass) {
        if (controller == null) {
            UIUtils.showErrorMessage(this, "Error", "Class Controller is not initialized.");
            return;
        }
        // Need lists of courses and teachers for the dialog dropdowns
        List<Course> courses = controller.getAllCoursesForSelection();
        List<Teacher> teachers = controller.getAllTeachersForSelection();

        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        ClassDialog dialog = new ClassDialog((Frame) parentWindow, controller, eduClass, courses, teachers);
        dialog.setVisible(true);
        // Refresh handled by controller
    }

    private void enrollStudentAction() {
        int selectedClassRow = classTable.getSelectedRow();
        if (selectedClassRow < 0) {
            UIUtils.showWarningMessage(this, "Selection Required", "Please select a class first.");
            return;
        }
        int modelRow = classTable.convertRowIndexToModel(selectedClassRow);
        int classId = (int) classTableModel.getValueAt(modelRow, 0);
        EduClass selectedClass = controller.getEduClassById(classId); // Get current class details

        if (selectedClass == null) return;

        // Check if class is full
        if(selectedClass.getCurrentEnrollment() >= selectedClass.getMaxCapacity()) {
            UIUtils.showWarningMessage(this, "Class Full", "The selected class '" + selectedClass.getClassName() + "' is already full.");
            return;
        }


        // Get list of available students (not already enrolled in this class)
        List<Student> availableStudents = controller.getAvailableStudentsForEnrollment(classId);
        if (availableStudents.isEmpty()) {
            UIUtils.showInfoMessage(this, "No Students", "There are no available students to enroll in this class.");
            return;
        }

        // Create a list model for the JList
        DefaultListModel<Student> listModel = new DefaultListModel<>();
        availableStudents.forEach(listModel::addElement); // Assumes Student has a reasonable toString()

        // Create JList and ScrollPane
        JList<Student> studentList = new JList<>(listModel);
        studentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Customize rendering if needed to show more info than toString()
        studentList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Student) {
                    Student s = (Student) value;
                    setText(s.getFullName() + " (ID: " + s.getStudentId() + ")");
                }
                return c;
            }
        });

        JScrollPane listScrollPane = new JScrollPane(studentList);
        listScrollPane.setPreferredSize(new Dimension(300, 200));

        // Show dialog with the list
        int result = JOptionPane.showConfirmDialog(this, listScrollPane, "Select Student to Enroll",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            Student selectedStudent = studentList.getSelectedValue();
            if (selectedStudent != null) {
                // Call controller to enroll
                controller.enrollStudent(classId, selectedStudent.getStudentId());
                // Student table refresh is handled by controller/refreshStudentList method
            } else {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select a student from the list.");
            }
        }
    }

    private void unenrollStudentAction() {
        int selectedClassRow = classTable.getSelectedRow();
        int selectedStudentRow = enrolledStudentTable.getSelectedRow();

        if (selectedClassRow < 0) {
            UIUtils.showWarningMessage(this, "Class Selection Required", "Please select a class first.");
            return;
        }
        if (selectedStudentRow < 0) {
            UIUtils.showWarningMessage(this, "Student Selection Required", "Please select a student from the enrolled list to unenroll.");
            return;
        }

        int classModelRow = classTable.convertRowIndexToModel(selectedClassRow);
        int classId = (int) classTableModel.getValueAt(classModelRow, 0);

        // Student table doesn't have sorter here, model row = view row
        int studentId = (int) studentTableModel.getValueAt(selectedStudentRow, 0); // Student ID is column 0
        String studentName = (String) studentTableModel.getValueAt(selectedStudentRow, 1);

        if (UIUtils.showConfirmDialog(this, "Confirm Unenrollment", "Are you sure you want to unenroll student '" + studentName + "' (ID: " + studentId + ")?")) {
            controller.unenrollStudent(classId, studentId);
            // Student table refresh handled by controller/refreshStudentList method
        }

    }


    public void refreshTable() {
        if (controller == null) return;
        List<EduClass> classes = controller.getAllEduClasses();
        populateClassTable(classes);
        if (classTableModel != null) { // Sử dụng classTableModel
            classTableModel.fireTableDataChanged(); // Thông báo cho bảng lớp cập nhật
            System.out.println(this.getClass().getSimpleName() + ": fireTableDataChanged() called for classTableModel.");
        }
        // Clear student list when class table is refreshed
        studentTableModel.setRowCount(0);
        selectedClassLabel.setText("Select a class to see enrolled students.");
        selectedClassLabel.setFont(selectedClassLabel.getFont().deriveFont(Font.ITALIC));
        enrollStudentButton.setEnabled(false);
        unenrollStudentButton.setEnabled(false);
    }

    // Called when a class selection changes or after enrollment actions
    public void refreshStudentListForSelectedClass() {
        int selectedRow = classTable.getSelectedRow();
        if (selectedRow < 0 || controller == null) {
            studentTableModel.setRowCount(0); // Clear if no selection
            return;
        }
        int modelRow = classTable.convertRowIndexToModel(selectedRow);
        int classId = (int) classTableModel.getValueAt(modelRow, 0);
        List<Student> students = controller.getEnrolledStudents(classId);
        populateStudentTable(students);
        if (studentTableModel != null) {
            studentTableModel.fireTableDataChanged(); // Thông báo cho bảng student cập nhật
            System.out.println(this.getClass().getSimpleName() + ": fireTableDataChanged() called for studentTableModel.");
        }
    }


    private void populateClassTable(List<EduClass> classes) {
        classTableModel.setRowCount(0); // Clear existing data
        if (classes != null) {
            for (EduClass eduClass : classes) {
                Vector<Object> row = new Vector<>();
                row.add(eduClass.getClassId());
                row.add(eduClass.getClassName());
                // Display Course Code/Name and Teacher Name instead of objects
                row.add(eduClass.getCourse() != null ? eduClass.getCourse().getCourseCode() : "N/A");
                row.add(eduClass.getPrimaryTeacher() != null ? eduClass.getPrimaryTeacher().getFullName() : "N/A");
                row.add(eduClass.getAcademicYear());
                row.add(eduClass.getSemester());
                row.add(eduClass.getMaxCapacity());
                row.add(eduClass.getCurrentEnrollment());
                classTableModel.addRow(row);
            }
        }
    }

    private void populateStudentTable(List<Student> students) {
        studentTableModel.setRowCount(0); // Clear existing data
        if (students != null) {
            for (Student student : students) {
                Vector<Object> row = new Vector<>();
                row.add(student.getStudentId());
                row.add(student.getFullName());
                studentTableModel.addRow(row);
            }
        }
    }

    public void setAdminControlsEnabled(boolean isAdmin) {
        addClassButton.setVisible(isAdmin);
        editClassButton.setVisible(isAdmin);
        deleteClassButton.setVisible(isAdmin);
        // Nút Enroll/Unenroll có thể cần logic khác (ví dụ: cả Admin và Teacher)
        enrollStudentButton.setVisible(isAdmin); // Hoặc logic phức tạp hơn
        unenrollStudentButton.setVisible(isAdmin);
    }
}