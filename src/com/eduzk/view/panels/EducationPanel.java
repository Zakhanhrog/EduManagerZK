package com.eduzk.view.panels;

import com.eduzk.controller.EducationController;
import com.eduzk.controller.MainController;
import com.eduzk.model.entities.AcademicRecord;
import com.eduzk.model.entities.EduClass;
import com.eduzk.model.entities.Role;
import com.eduzk.model.entities.Student;
import com.eduzk.model.entities.ArtStatus;
import com.eduzk.model.entities.ConductRating;
import com.eduzk.utils.UIUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

public class EducationPanel extends JPanel {

    private EducationController controller;
    private MainController mainController;
    private Role currentUserRole;

    private JSplitPane splitPane;
    private JPanel leftPanel;
    private JPanel rightPanel;

    private JTree classTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode mainRootNode; // Thêm nút gốc chính
    private DefaultMutableTreeNode resultsNode;
    private DefaultMutableTreeNode assignmentsNode;
    private JScrollPane treeScrollPane;

    private JTable gradeTable;
    private DefaultTableModel gradeTableModel;
    private JScrollPane gradeTableScrollPane;
    private JButton exportButton;
    private JLabel selectedClassLabel;

    private boolean isEditing = false;
    private JButton editButton;
    private JButton cancelEditButton;
    private JButton saveChangesButton;

    private static final DecimalFormat df = new DecimalFormat("#.00");
    private static final String[] SUBJECT_KEYS = {"Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD", "Nghệ thuật"};
    private static final String[] EDITABLE_SUBJECT_KEYS = {"Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD"};
    private static final String ART_KEY = "Nghệ thuật";
    private static final String CONDUCT_KEY = "Hạnh kiểm";
    private static final String[] TABLE_COLUMNS = {"STT", "Tên HS", "Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD", ART_KEY, "TB KHTN", "TB KHXH", "TB môn học", CONDUCT_KEY};

    private boolean hasPendingChanges = false;
    private Icon resultsRootIcon;
    private Icon assignmentIcon;

    public EducationPanel() {
        super(new BorderLayout());
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setController(EducationController controller, Role userRole) {
        this.controller = controller;
        this.currentUserRole = userRole;
        if (this.controller != null) {
            this.controller.setEducationPanel(this);
            initComponents();
            setupLayout();
            setupActions();
            configureControlsForRole(this.currentUserRole);
        } else {
            this.removeAll();
            this.add(new JLabel("Education module not available.", SwingConstants.CENTER));
            this.revalidate();
            this.repaint();
        }
    }

    private void initComponents() {
        editButton = new JButton("Chỉnh Sửa");
        editButton.setIcon(UIUtils.loadSVGIcon("/icons/edit.svg", 20));
        cancelEditButton = new JButton("Hủy Bỏ");
        saveChangesButton = new JButton("Lưu Thay Đổi");
        saveChangesButton.setIcon(UIUtils.loadSVGIcon("/icons/save.svg", 20));
        exportButton = new JButton("Xuất Excel");
        exportButton.setIcon(UIUtils.loadSVGIcon("/icons/export.svg", 20));

        resultsRootIcon = UIUtils.loadSVGIcon("/icons/results.svg", 33);
        assignmentIcon = UIUtils.loadSVGIcon("/icons/assignment.svg", 33);

        editButton.setEnabled(false);
        saveChangesButton.setVisible(false);
        cancelEditButton.setVisible(false);
        exportButton.setEnabled(false);

        leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        mainRootNode = new DefaultMutableTreeNode("Education Root");
        resultsNode = new DefaultMutableTreeNode("Results & reviews");
        assignmentsNode = new DefaultMutableTreeNode("Assignments");
        mainRootNode.add(resultsNode);
        mainRootNode.add(assignmentsNode);

        treeModel = new DefaultTreeModel(mainRootNode);
        classTree = new JTree(treeModel);
        classTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        classTree.setRootVisible(false);
        classTree.setShowsRootHandles(true);
        classTree.setCellRenderer(new EduClassTreeCellRenderer(resultsRootIcon, assignmentIcon));
        treeScrollPane = new JScrollPane(classTree);
        treeScrollPane.setBorder(BorderFactory.createEmptyBorder());
        rightPanel = new JPanel(new BorderLayout(5, 10));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));


        selectedClassLabel = new JLabel("Select an item or class", SwingConstants.CENTER);
        selectedClassLabel.setFont(selectedClassLabel.getFont().deriveFont(Font.BOLD));

        gradeTableModel = new DefaultTableModel(TABLE_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (isEditing && controller != null && controller.canCurrentUserEdit()) {
                    String colName = getColumnName(column);
                    for (String key : EDITABLE_SUBJECT_KEYS) {
                        if (key.equals(colName)) return true;
                    }
                    return ART_KEY.equals(colName) || CONDUCT_KEY.equals(colName);
                }
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                String colName = getColumnName(columnIndex);
                if (ART_KEY.equals(colName)) return ArtStatus.class;
                if (CONDUCT_KEY.equals(colName)) return ConductRating.class;
                if (columnIndex <= 1) return String.class;
                return Double.class;
            }
        };

        gradeTable = new JTable(gradeTableModel);
        gradeTable.setRowHeight(25);
        gradeTable.getTableHeader().setReorderingAllowed(false);
        gradeTable.setCellSelectionEnabled(true);
        gradeTable.setRowSelectionAllowed(false);
        gradeTable.setColumnSelectionAllowed(false);
        JTableHeader header = gradeTable.getTableHeader();
        if (header != null) {
            header.setFont(header.getFont().deriveFont(Font.BOLD));
            DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) header.getDefaultRenderer();
            if (headerRenderer != null) {
                headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
            }
        }
        gradeTable.setShowGrid(true);
        gradeTable.setGridColor(Color.LIGHT_GRAY);

        setupTableEditorsAndRenderers();
        gradeTableScrollPane = new JScrollPane(gradeTable);
        gradeTableScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.15);
        splitPane.setDividerSize(8);
    }

    private void setupTableEditorsAndRenderers() {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer rightNumberRenderer = new DefaultTableCellRenderer() {
            private final DecimalFormat avgFormat = new DecimalFormat("#.00");

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.RIGHT);
                if (value instanceof Number) {
                    label.setText(avgFormat.format(((Number) value).doubleValue()));
                } else {
                    label.setText("");
                }
                return label;
            }
        };

        int tenHsColIndex = getColumnIndex("Tên HS");
        if (tenHsColIndex != -1) {
            TableColumn tenHsColumn = gradeTable.getColumnModel().getColumn(tenHsColIndex);
            tenHsColumn.setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus,
                                                               int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                    return c;
                }
            });
        }

        for (int i = 0; i < gradeTable.getColumnCount(); i++) {
            TableColumn column = gradeTable.getColumnModel().getColumn(i);
            String colName = gradeTable.getColumnName(i);

            if (colName.equals("Tên HS")) {
                continue;
            } else if (colName.equals("STT")) {
                column.setCellRenderer(centerRenderer);
            } else if (colName.equals(ART_KEY) || colName.equals(CONDUCT_KEY)) {
                DefaultTableCellRenderer enumRenderer = new DefaultTableCellRenderer();
                enumRenderer.setHorizontalAlignment(SwingConstants.CENTER);
                column.setCellRenderer(enumRenderer);
                if (colName.equals(ART_KEY)) {
                    JComboBox<ArtStatus> artComboBox = new JComboBox<>(ArtStatus.values());
                    column.setCellEditor(new DefaultCellEditor(artComboBox));
                } else {
                    JComboBox<ConductRating> conductComboBox = new JComboBox<>(ConductRating.values());
                    column.setCellEditor(new DefaultCellEditor(conductComboBox));
                }
            } else {
                column.setCellRenderer(rightNumberRenderer);
                if (List.of(EDITABLE_SUBJECT_KEYS).contains(colName)) {
                    JTextField numberTextField = new JTextField();
                    numberTextField.setHorizontalAlignment(JTextField.RIGHT);
                    DefaultCellEditor numberEditor = new DefaultCellEditor(numberTextField);
                    column.setCellEditor(numberEditor);
                }
            }
        }
        setColumnWidths();
    }

    private <E extends Enum<E>> void setupEnumColumn(String columnName, E[] enumValues) {
        int columnIndex = getColumnIndex(columnName);
        if (columnIndex != -1) {
            TableColumn column = gradeTable.getColumnModel().getColumn(columnIndex);
            JComboBox<E> comboBox = new JComboBox<>(enumValues);
            column.setCellEditor(new DefaultCellEditor(comboBox));
        }
    }

    private void setColumnWidths(){
        try {
            gradeTable.getColumnModel().getColumn(getColumnIndex("STT")).setMaxWidth(40);
            gradeTable.getColumnModel().getColumn(getColumnIndex("Tên HS")).setPreferredWidth(180);
            gradeTable.getColumnModel().getColumn(getColumnIndex(ART_KEY)).setMinWidth(100);
            gradeTable.getColumnModel().getColumn(getColumnIndex(CONDUCT_KEY)).setMinWidth(100);
            gradeTable.getColumnModel().getColumn(getColumnIndex("TB KHTN")).setPreferredWidth(80);
            gradeTable.getColumnModel().getColumn(getColumnIndex("TB KHXH")).setPreferredWidth(80);
            gradeTable.getColumnModel().getColumn(getColumnIndex("TB môn học")).setPreferredWidth(90);
            for(String key : EDITABLE_SUBJECT_KEYS) {
                int colIdx = getColumnIndex(key);
                if (colIdx != -1) {
                    gradeTable.getColumnModel().getColumn(colIdx).setPreferredWidth(60);
                }
            }
        } catch (Exception e) {
            System.err.println("Error setting column widths: " + e.getMessage());
        }
    }

    private void setupLayout() {
        leftPanel.add(treeScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.add(editButton);
        buttonPanel.add(cancelEditButton);
        buttonPanel.add(saveChangesButton);
        buttonPanel.add(exportButton);

        JPanel topOfRightPanel = new JPanel(new BorderLayout(10,0));
        topOfRightPanel.add(selectedClassLabel, BorderLayout.CENTER);
        topOfRightPanel.add(buttonPanel, BorderLayout.EAST);
        topOfRightPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        rightPanel.add(topOfRightPanel, BorderLayout.NORTH);
        rightPanel.add(gradeTableScrollPane, BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private void setupActions() {
        classTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) classTree.getLastSelectedPathComponent();

                if (selectedNode == null) {
                    showPlaceholderView("Please select an item from the left.");
                    return;
                }

                Object userObject = selectedNode.getUserObject();

                if (userObject instanceof String) {
                    String nodeText = (String) userObject;
                    if ("Results & reviews".equals(nodeText)) {
                        showPlaceholderView("Select a class under 'Results & reviews' to view grades.");
                        if (controller != null) controller.clearSelectedClass();
                        setEditingMode(false);
                        editButton.setEnabled(false);
                        exportButton.setEnabled(false);
                        markChangesPending(false);
                    } else if ("Assignments".equals(nodeText)) {
                        showPlaceholderView("Assignment management feature is under development.");
                        if (controller != null) controller.clearSelectedClass();
                        setEditingMode(false);
                        editButton.setEnabled(false);
                        exportButton.setEnabled(false);
                        markChangesPending(false);
                    }
                } else if (userObject instanceof EduClass) {
                    showGradeTableView();
                    if (isEditing && hasPendingChanges) {
                        if (!UIUtils.showConfirmDialog(EducationPanel.this, "Unsaved Changes", "Discard unsaved changes?")) {
                            return;
                        }
                    }
                    setEditingMode(false);
                    EduClass selectedClass = (EduClass) userObject;
                    selectedClassLabel.setText("Class: " + selectedClass.getClassName());
                    if (controller != null) {
                        controller.loadDataForClass(selectedClass.getClassId());
                        boolean canEdit = controller.canCurrentUserEdit();
                        boolean hasData = gradeTableModel.getRowCount() > 0;
                        editButton.setEnabled(canEdit && hasData);
                        exportButton.setEnabled(hasData);
                        markChangesPending(false);
                    }
                }
            }
        });

        gradeTableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && isEditing) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                if (controller != null && row >= 0 && column >= 0 ) {
                    String columnName = gradeTableModel.getColumnName(column);
                    boolean isDataColumn = false;
                    for(String key : EDITABLE_SUBJECT_KEYS) { if (key.equals(columnName)) {isDataColumn = true; break;} }
                    if(!isDataColumn) isDataColumn = ART_KEY.equals(columnName) || CONDUCT_KEY.equals(columnName);

                    if(isDataColumn) {
                        Object newValue = gradeTableModel.getValueAt(row, column);
                        System.out.println("Table cell edited: row=" + row + ", col=" + column + ", name=" + columnName + ", value=" + newValue);
                        controller.updateRecordInMemory(row, columnName, newValue);
                    }
                }
            }
        });

        editButton.addActionListener(e -> {
            setEditingMode(true);
            UIUtils.showInfoMessage(this, "Edit Mode", "You can now edit grades and conduct.");
        });

        cancelEditButton.addActionListener(e -> {
            if (hasPendingChanges) {
                if (!UIUtils.showConfirmDialog(this, "Discard Changes?", "Are you sure you want to discard all unsaved changes?")) {
                    return;
                }
            }
            setEditingMode(false);
            if(controller != null && controller.getCurrentSelectedClassId() > 0){
                System.out.println("Cancelling edit, reloading data for class: " + controller.getCurrentSelectedClassId());
                controller.loadDataForClass(controller.getCurrentSelectedClassId());
            }
        });

        saveChangesButton.addActionListener(e -> {
            if (controller != null) {
                controller.saveAllChanges();
                if (!hasPendingChanges) {
                    setEditingMode(false);
                }
            }
        });

        exportButton.addActionListener(e -> {
            if (controller != null && mainController != null && controller.getCurrentSelectedClassId() > 0) {
                mainController.requestExcelExport(MainController.EXPORT_GRADES, controller.getCurrentSelectedClassId());
            } else {
                if(mainController == null) System.err.println("Export Error: MainController reference is null in EducationPanel.");
                UIUtils.showWarningMessage(this, "Cannot Export", "Please select a class first.");
            }
        });
    }

    public void configureControlsForRole(Role userRole) {
        this.currentUserRole = userRole;
        System.out.println("EducationPanel: Configuring controls for role: " + userRole);

        boolean canEdit = (controller != null && controller.canCurrentUserEdit());

        if (splitPane == null || leftPanel == null || rightPanel == null || editButton == null || cancelEditButton == null || saveChangesButton == null || exportButton == null) {
            System.err.println("EducationPanel: Components not fully initialized in configureControlsForRole.");
            return;
        }

        switch (userRole) {
            case ADMIN:
            case TEACHER:
                splitPane.setLeftComponent(leftPanel);
                splitPane.setDividerSize(8);
                splitPane.setVisible(true);
                leftPanel.setVisible(true);
                showGradeTableView();
                reloadClassTree();
                setEditingMode(false);

                TreePath selectionPath = classTree.getSelectionPath();
                boolean classSelected = (selectionPath != null && selectionPath.getLastPathComponent() instanceof DefaultMutableTreeNode && ((DefaultMutableTreeNode)selectionPath.getLastPathComponent()).getUserObject() instanceof EduClass);
                boolean hasData = (gradeTableModel != null && gradeTableModel.getRowCount() > 0);

                editButton.setVisible(true);
                exportButton.setVisible(true);
                cancelEditButton.setVisible(isEditing);
                saveChangesButton.setVisible(isEditing);

                editButton.setEnabled(classSelected && canEdit && hasData);
                exportButton.setEnabled(classSelected && hasData);
                saveChangesButton.setEnabled(isEditing && hasPendingChanges && canEdit);
                break;

            case STUDENT:
                splitPane.setLeftComponent(null);
                splitPane.setDividerSize(0);
                splitPane.setVisible(true);
                leftPanel.setVisible(false);
                showGradeTableView();
                if(controller != null) controller.loadDataForCurrentStudent();
                setEditingMode(false);
                editButton.setVisible(false);
                cancelEditButton.setVisible(false);
                saveChangesButton.setVisible(false);
                exportButton.setVisible(false);
                if (rightPanel != null) {
                    Component topComponent = ((BorderLayout)rightPanel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
                    if (topComponent != null) rightPanel.remove(topComponent);
                    rightPanel.setBorder(BorderFactory.createTitledBorder("Bảng điểm cá nhân"));
                }
                break;

            default:
                splitPane.setVisible(false);
                this.removeAll();
                this.add(new JLabel("Access Restricted.", SwingConstants.CENTER));
                break;
        }
        this.revalidate();
        this.repaint();
    }

    public void reloadClassTree() {
        if (controller != null && treeModel != null && resultsNode != null) {
            System.out.println("EducationPanel: Reloading class tree.");

            TreePath selectedPath = classTree.getSelectionPath();
            EduClass selectedClassBefore = null;
            if (selectedPath != null && selectedPath.getLastPathComponent() instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
                if (selectedNode.getUserObject() instanceof EduClass) {
                    selectedClassBefore = (EduClass) selectedNode.getUserObject();
                }
            }

            resultsNode.removeAllChildren();
            List<EduClass> classes = controller.getClassesForCurrentUser();
            DefaultMutableTreeNode nodeToSelectAfter = null;

            if (classes != null) {
                for (EduClass eduClass : classes) {
                    DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(eduClass);
                    resultsNode.add(classNode);
                    if (selectedClassBefore != null && eduClass.getClassId() == selectedClassBefore.getClassId()) {
                        nodeToSelectAfter = classNode;
                    }
                }
            }

            treeModel.reload(resultsNode);
            classTree.expandPath(new TreePath(resultsNode.getPath()));

            if (nodeToSelectAfter != null) {
                TreePath path = new TreePath(nodeToSelectAfter.getPath());
                classTree.setSelectionPath(path);
                classTree.scrollPathToVisible(path);
                boolean canEdit = (controller != null && controller.canCurrentUserEdit());
                editButton.setEnabled(canEdit);
                exportButton.setEnabled(gradeTableModel.getRowCount() > 0);
            } else {
                classTree.setSelectionPath(new TreePath(resultsNode.getPath()));
                selectedClassLabel.setText("Results & reviews");
                gradeTableModel.setRowCount(0);
                setEditingMode(false);
                editButton.setEnabled(false);
                exportButton.setEnabled(false);
                markChangesPending(false);
            }
            System.out.println("EducationPanel: Class tree reloaded.");
        }
    }

    public void updateTableData(List<Student> students, List<AcademicRecord> records) {
        gradeTableModel.setRowCount(0);
        if (students == null || records == null || students.size() != records.size()) {
            System.err.println("Error updating table: Mismatch/null student/record lists.");
            setEditingMode(false);
            if(editButton != null) editButton.setEnabled(false);
            if(exportButton != null) exportButton.setEnabled(false);
            return;
        }

        for (int i = 0; i < students.size(); i++) {
            Student student = students.get(i);
            AcademicRecord record = records.get(i);
            Vector<Object> row = new Vector<>();
            row.add(i + 1);
            row.add(student.getFullName());
            row.add(record.getGrade("Toán"));
            row.add(record.getGrade("Văn"));
            row.add(record.getGrade("Anh"));
            row.add(record.getGrade("Lí"));
            row.add(record.getGrade("Hoá"));
            row.add(record.getGrade("Sinh"));
            row.add(record.getGrade("Sử"));
            row.add(record.getGrade("Địa"));
            row.add(record.getGrade("GDCD"));
            row.add(record.getArtStatus());
            row.add(record.calculateAvgNaturalSciences());
            row.add(record.calculateAvgSocialSciences());
            row.add(record.calculateAvgOverallSubjects());
            row.add(record.getConductRating());
            gradeTableModel.addRow(row);
        }
        markChangesPending(false);
        setEditingMode(false);
        boolean canEdit = (controller != null && controller.canCurrentUserEdit());
        if(editButton != null) editButton.setEnabled(canEdit && gradeTableModel.getRowCount() > 0);
        if(exportButton != null) exportButton.setEnabled(gradeTableModel.getRowCount() > 0);
    }

    public void updateTableDataForStudent(Student student, List<AcademicRecord> records) {
        gradeTableModel.setRowCount(0);
        if (student == null || records == null) return;
        for (AcademicRecord record : records) {
            Vector<Object> row = new Vector<>();
            row.add(1);
            row.add(student.getFullName());
            row.add(record.getGrade("Toán"));
            row.add(record.getGrade("Văn"));
            row.add(record.getGrade("Anh"));
            row.add(record.getGrade("Lí"));
            row.add(record.getGrade("Hoá"));
            row.add(record.getGrade("Sinh"));
            row.add(record.getGrade("Sử"));
            row.add(record.getGrade("Địa"));
            row.add(record.getGrade("GDCD"));
            row.add(record.getArtStatus());
            row.add(record.calculateAvgNaturalSciences());
            row.add(record.calculateAvgSocialSciences());
            row.add(record.calculateAvgOverallSubjects());
            row.add(record.getConductRating());
            gradeTableModel.addRow(row);
        }
        setEditingMode(false);
        if(editButton != null) editButton.setVisible(false);
        if(cancelEditButton != null) cancelEditButton.setVisible(false);
        if(saveChangesButton != null) saveChangesButton.setVisible(false);
        if(exportButton != null) exportButton.setVisible(false);
    }

    public void updateCalculatedValues(int rowIndex, AcademicRecord record) {
        if (rowIndex >= 0 && rowIndex < gradeTableModel.getRowCount()) {
            try {
                gradeTableModel.setValueAt(record.calculateAvgNaturalSciences(), rowIndex, getColumnIndex("TB KHTN"));
                gradeTableModel.setValueAt(record.calculateAvgSocialSciences(), rowIndex, getColumnIndex("TB KHXH"));
                gradeTableModel.setValueAt(record.calculateAvgOverallSubjects(), rowIndex, getColumnIndex("TB môn học"));
            } catch(ArrayIndexOutOfBoundsException e) {
                System.err.println("Error updating calculated values, column index likely invalid: " + e.getMessage());
            }
        }
    }

    public void markChangesPending(boolean pending) {
        this.hasPendingChanges = pending;
        if (saveChangesButton != null) {
            saveChangesButton.setEnabled(isEditing && pending && controller != null && controller.canCurrentUserEdit());
        }
    }

    public void refreshTableCell(int rowIndex, String subjectKey){
        if(rowIndex >= 0 && rowIndex < gradeTableModel.getRowCount()){
            int columnIndex = getColumnIndex(subjectKey);
            if(columnIndex != -1){
                System.out.println("Requesting repaint for cell: row=" + rowIndex + ", col=" + columnIndex);
                gradeTableModel.fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }

    public int getColumnIndex(String columnName) {
        if (gradeTable == null) return -1;
        for (int i = 0; i < gradeTable.getColumnCount(); i++) {
            if (gradeTable.getColumnName(i).equals(columnName)) {
                return i;
            }
        }
        System.err.println("Warning: Column not found in gradeTable: " + columnName);
        return -1;
    }

    public DefaultTableModel getGradeTableModel() {
        return gradeTableModel;
    }

    private void showPlaceholderView(String message) {
        rightPanel.removeAll();
        JLabel placeholder = new JLabel("<html><center><i>" + message + "</i></center></html>", SwingConstants.CENTER);
        placeholder.setForeground(Color.GRAY);
        placeholder.setFont(placeholder.getFont().deriveFont(14f));
        rightPanel.add(placeholder, BorderLayout.CENTER);
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    private void showGradeTableView() {
        rightPanel.removeAll();

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.add(editButton);
        buttonPanel.add(cancelEditButton);
        buttonPanel.add(saveChangesButton);
        buttonPanel.add(exportButton);

        JPanel topOfRightPanel = new JPanel(new BorderLayout(10,0));
        topOfRightPanel.add(selectedClassLabel, BorderLayout.CENTER);
        topOfRightPanel.add(buttonPanel, BorderLayout.EAST);
        topOfRightPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        rightPanel.add(topOfRightPanel, BorderLayout.NORTH);
        rightPanel.add(gradeTableScrollPane, BorderLayout.CENTER);

        rightPanel.revalidate();
        rightPanel.repaint();
    }

    private class EduClassTreeCellRenderer extends DefaultTreeCellRenderer {
        private final Icon rootIcon;
        private final Icon assignmentIcon;

        public EduClassTreeCellRenderer(Icon rootIcon, Icon assignmentIcon) {
            this.rootIcon = rootIcon;
            this.assignmentIcon = assignmentIcon;
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();

                if (userObject instanceof String) {
                    String nodeText = (String) userObject;
                    label.setText(nodeText);
                    if ("Results & reviews".equals(nodeText)) {
                        if (this.rootIcon != null) {
                            label.setIcon(this.rootIcon);
                        } else {
                            label.setIcon(UIManager.getIcon(expanded ? "Tree.openIcon" : "Tree.closedIcon"));
                        }
                    } else if ("Assignments".equals(nodeText)) {
                        if (this.assignmentIcon != null) {
                            label.setIcon(this.assignmentIcon);
                        } else {
                            label.setIcon(UIManager.getIcon(expanded ? "Tree.openIcon" : "Tree.closedIcon"));
                        }
                    } else {
                        label.setIcon(UIManager.getIcon(expanded ? "Tree.openIcon" : "Tree.closedIcon"));
                    }
                } else if (userObject instanceof EduClass) {
                    EduClass eduClass = (EduClass) userObject;
                    label.setText(eduClass.getClassName());
                    label.setIcon(UIManager.getIcon("Tree.leafIcon"));
                } else {
                    label.setText(userObject == null ? "" : userObject.toString());
                }
            } else {
                label.setText(value == null ? "" : value.toString());
            }
            return label;
        }
    }

    private void setEditingMode(boolean editing) {
        this.isEditing = editing;
        if (editButton != null) {
            editButton.setVisible(!editing);
            editButton.setEnabled(!editing && controller != null && controller.canCurrentUserEdit() && (gradeTableModel != null && gradeTableModel.getRowCount() > 0));
        }
        if (cancelEditButton != null) {
            cancelEditButton.setVisible(editing);
        }
        if (saveChangesButton != null) {
            saveChangesButton.setVisible(editing);
            saveChangesButton.setEnabled(editing && hasPendingChanges && controller != null && controller.canCurrentUserEdit());
        }
        if (gradeTable != null) {
            gradeTable.repaint();
        }
        System.out.println("Editing mode set to: " + editing);
    }

    public void updateSpecificCellValue(int rowIndex, String subjectKey, Object value) {
        int columnIndex = getColumnIndex(subjectKey);
        if (rowIndex >= 0 && rowIndex < gradeTableModel.getRowCount() && columnIndex != -1) {
            SwingUtilities.invokeLater(() -> {
                System.out.println("Updating TableModel cell: row=" + rowIndex + ", col=" + columnIndex + ", value=" + value);
                gradeTableModel.setValueAt(value, rowIndex, columnIndex);
            });
        } else {
            System.err.println("Error updating specific cell value: Invalid row/column index or key not found. Row: " + rowIndex + ", Key: " + subjectKey);
        }
    }
}