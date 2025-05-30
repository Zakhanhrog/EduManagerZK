package com.eduzk.view.panels;

import com.eduzk.controller.EduClassController;
import com.eduzk.model.entities.Course;
import com.eduzk.model.entities.EduClass;
import com.eduzk.model.entities.Student;
import com.eduzk.model.entities.Teacher;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.dialogs.ClassDialog;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.Vector;
import javax.swing.Icon;
import java.net.URL;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ClassPanel extends JPanel {

    private EduClassController controller;
    private JTable classTable;
    private DefaultTableModel classTableModel;
    private JTable enrolledStudentTable;
    private DefaultTableModel studentTableModel;
    private JButton addClassButton, editClassButton, deleteClassButton;
    private JButton enrollStudentButton, unenrollStudentButton;
    private JSplitPane splitPane;
    private JLabel selectedClassLabel;
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
        refreshTable();
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
        enrolledStudentTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // --- Buttons ---
        int iconSize = 20;
        addClassButton = new JButton("Add Class");
        Icon addIcon = loadSVGIconButton("/icons/add.svg", iconSize);
        if (addIcon != null) addClassButton.setIcon(addIcon);

        editClassButton = new JButton("Edit Class");
        Icon editIcon = loadSVGIconButton("/icons/edit.svg", iconSize);
        if (editIcon != null) editClassButton.setIcon(editIcon);

        deleteClassButton = new JButton("Delete Class");
        Icon deleteIcon = loadSVGIconButton("/icons/delete.svg", iconSize);
        if (deleteIcon != null) deleteClassButton.setIcon(deleteIcon);

        refreshButton = new JButton("Refresh");
        Icon refreshIcon = loadSVGIconButton("/icons/refresh.svg", iconSize);
        if (refreshIcon != null) refreshButton.setIcon(refreshIcon);
        refreshButton.setToolTipText("Reload class data from storage");

        enrollStudentButton = new JButton("Enroll Students");
        unenrollStudentButton = new JButton("Unenroll Student");
        enrollStudentButton.setEnabled(false);
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
        leftPanel.setMinimumSize(new Dimension(400, 200));

        // --- Right Panel (Student List and Actions) ---
        JPanel studentActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        studentActionPanel.add(enrollStudentButton);
        studentActionPanel.add(unenrollStudentButton);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 5));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Enrolled Students"));
        rightPanel.add(selectedClassLabel, BorderLayout.NORTH);
        JScrollPane studentScrollPane = new JScrollPane(enrolledStudentTable);
        rightPanel.add(studentScrollPane, BorderLayout.CENTER);
        rightPanel.add(studentActionPanel, BorderLayout.SOUTH);
        rightPanel.setMinimumSize(new Dimension(300, 200));

        // --- Split Pane ---
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.6);

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

        enrollStudentButton.addActionListener(e -> enrollMultipleStudentsAction());
        unenrollStudentButton.addActionListener(e -> unenrollMultipleStudentsAction());
        refreshButton.addActionListener(e -> {
            System.out.println("ClassPanel: Refresh button clicked.");
            refreshTable(); // Gọi lại chính phương thức refresh của panel này
            UIUtils.showInfoMessage(this,"Refreshed", "Class list updated."); // Thông báo (tùy chọn)
        });

        classTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = classTable.getSelectedRow();
                    boolean classSelected = (selectedRow >= 0);
                    enrollStudentButton.setEnabled(classSelected);
                    updateUnenrollButtonState();

                    if (classSelected) {
                        int modelRow = classTable.convertRowIndexToModel(selectedRow);
                        int classId = (int) classTableModel.getValueAt(modelRow, 0);
                        String className = (String) classTableModel.getValueAt(modelRow, 1);
                        selectedClassLabel.setText("Students in: " + className);
                        selectedClassLabel.setFont(selectedClassLabel.getFont().deriveFont(Font.BOLD));
                        refreshStudentListForSelectedClass();
                    } else {
                        selectedClassLabel.setText("Select a class to see enrolled students.");
                        selectedClassLabel.setFont(selectedClassLabel.getFont().deriveFont(Font.ITALIC));
                        studentTableModel.setRowCount(0);
                    }
                }
            }
        });
        enrolledStudentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateUnenrollButtonState();
            }
        });
    }

    private void updateUnenrollButtonState() {
        boolean classSelected = (classTable.getSelectedRow() >= 0);
        boolean studentSelected = (enrolledStudentTable.getSelectedRowCount() > 0);
        unenrollStudentButton.setEnabled(classSelected && studentSelected);
    }

    private void enrollMultipleStudentsAction() {
        int selectedClassRow = classTable.getSelectedRow();
        if (selectedClassRow < 0) {
            return;
        }
        int modelRow = classTable.convertRowIndexToModel(selectedClassRow);
        int classId = (int) classTableModel.getValueAt(modelRow, 0);
        EduClass selectedClass = controller.getEduClassById(classId);
        if (selectedClass == null) return;

        List<Student> availableStudents = controller.getAvailableStudentsForEnrollment(classId);
        if (availableStudents.isEmpty()) {  return; }

        DefaultListModel<Student> listModel = new DefaultListModel<>();
        availableStudents.forEach(listModel::addElement);
        JList<Student> studentList = new JList<>(listModel);

        studentList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        studentList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof Student) {
                    Student student = (Student) value;
                    String displayText = String.valueOf(student.getStudentId()) + " - " + student.getFullName();
                    ((JLabel) label).setText(displayText);
                }
                return label;
            }
        });
        JScrollPane listScrollPane = new JScrollPane(studentList);
        listScrollPane.setPreferredSize(new Dimension(350, 250));

        int result = JOptionPane.showConfirmDialog(this, listScrollPane, "Select Student(s) to Enroll",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            List<Student> selectedStudents = studentList.getSelectedValuesList();

            if (selectedStudents != null && !selectedStudents.isEmpty()) {
                int remainingCapacity = selectedClass.getMaxCapacity() - selectedClass.getCurrentEnrollment();
                if (selectedStudents.size() > remainingCapacity) {
                    UIUtils.showWarningMessage(this, "Capacity Exceeded",
                            "Cannot enroll " + selectedStudents.size() + " students. Only " +
                                    remainingCapacity + " spot(s) remaining in class '" + selectedClass.getClassName() + "'.");
                    return;
                }
                List<Integer> studentIdsToEnroll = selectedStudents.stream()
                        .map(Student::getStudentId)
                        .collect(Collectors.toList());
                controller.enrollStudents(classId, studentIdsToEnroll);
            } else {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select at least one student from the list.");
            }
        }
    }

    private void unenrollMultipleStudentsAction() {
        int selectedClassRow = classTable.getSelectedRow();
        int[] selectedStudentViewRows = enrolledStudentTable.getSelectedRows();
        if (selectedClassRow < 0) {
            return;
        }
        if (selectedStudentViewRows.length == 0) {
            return;
        }
        int classModelRow = classTable.convertRowIndexToModel(selectedClassRow);
        int classId = (int) classTableModel.getValueAt(classModelRow, 0);
        List<Integer> studentIdsToUnenroll = new ArrayList<>();
        List<String> studentNamesToUnenroll = new ArrayList<>();
        for (int viewRow : selectedStudentViewRows) {
            int modelRow = viewRow;
            studentIdsToUnenroll.add((Integer) studentTableModel.getValueAt(modelRow, 0));
            studentNamesToUnenroll.add((String) studentTableModel.getValueAt(modelRow, 1));
        }

        String confirmationMessage;
        if (studentIdsToUnenroll.size() == 1) {
            confirmationMessage = "Are you sure you want to unenroll student '" + studentNamesToUnenroll.get(0) + "' (ID: " + studentIdsToUnenroll.get(0) + ")?";
        } else {
            confirmationMessage = "Are you sure you want to unenroll these " + studentIdsToUnenroll.size() + " selected students?";
        }

        if (UIUtils.showConfirmDialog(this, "Confirm Unenrollment", confirmationMessage)) {
            controller.unenrollStudents(classId, studentIdsToUnenroll);
        }
    }

    private void openClassDialog(EduClass eduClass) {
        if (controller == null) {
            UIUtils.showErrorMessage(this, "Error", "Class Controller is not initialized.");
            return;
        }
        List<Course> courses = controller.getAllCoursesForSelection();
        List<Teacher> teachers = controller.getAllTeachersForSelection();

        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        ClassDialog dialog = new ClassDialog((Frame) parentWindow, controller, eduClass, courses, teachers);
        dialog.setVisible(true);
    }

    public void refreshTable() {
        if (controller == null) return;
        List<EduClass> classes = controller.getAllEduClasses();
        populateClassTable(classes);
        if (classTableModel != null) {
            classTableModel.fireTableDataChanged();
            System.out.println(this.getClass().getSimpleName() + ": fireTableDataChanged() called for classTableModel.");
        }
        studentTableModel.setRowCount(0);
        selectedClassLabel.setText("Select a class to see enrolled students.");
        selectedClassLabel.setFont(selectedClassLabel.getFont().deriveFont(Font.ITALIC));
        enrollStudentButton.setEnabled(false);
        unenrollStudentButton.setEnabled(false);
    }

    public void refreshStudentListForSelectedClass() {
        int selectedRow = classTable.getSelectedRow();
        if (selectedRow < 0 || controller == null) {
            studentTableModel.setRowCount(0);
            return;
        }
        int modelRow = classTable.convertRowIndexToModel(selectedRow);
        int classId = (int) classTableModel.getValueAt(modelRow, 0);
        List<Student> students = controller.getEnrolledStudents(classId);
        populateStudentTable(students);
        if (studentTableModel != null) {
            studentTableModel.fireTableDataChanged();
            System.out.println(this.getClass().getSimpleName() + ": fireTableDataChanged() called for studentTableModel.");
        }
    }


    private void populateClassTable(List<EduClass> classes) {
        classTableModel.setRowCount(0);
        if (classes != null) {
            for (EduClass eduClass : classes) {
                Vector<Object> row = new Vector<>();
                row.add(eduClass.getClassId());
                row.add(eduClass.getClassName());
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
        studentTableModel.setRowCount(0);
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
        enrollStudentButton.setVisible(isAdmin);
        unenrollStudentButton.setVisible(isAdmin);
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