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
import com.eduzk.model.entities.Assignment;
import javax.swing.table.DefaultTableCellRenderer;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class EducationPanel extends JPanel {

    private EducationController controller;
    private MainController mainController;
    private Role currentUserRole;

    private JSplitPane splitPane;
    private JPanel leftPanel;
    private JPanel rightPanel; // Will use CardLayout

    private JTree classTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode mainRootNode;
    private DefaultMutableTreeNode resultsNode;
    private DefaultMutableTreeNode assignmentsNode;
    private JScrollPane treeScrollPane;
    private Icon resultsRootIcon;
    private Icon assignmentIcon; // Icon for Assignment node in tree

    private JPanel gradePanel; // Panel to hold grade components within CardLayout
    private JTable gradeTable;
    private DefaultTableModel gradeTableModel;
    private JScrollPane gradeTableScrollPane;
    private JButton exportButton; // Export Grades
    private JLabel selectedClassLabel; // Label for selected class in Grade view
    private boolean isEditing = false; // Grade editing mode flag
    private JButton editButton; // Edit Grades
    private JButton cancelButton; // Cancel Grade edits
    private JButton saveChangesButton; // Save Grade changes
    private boolean hasPendingChanges = false; // Grade changes pending flag
    private JPanel gradeButtonPanel;
    private JPanel topOfGradePanel;

    private JPanel assignmentManagementPanel; // Panel to hold assignment components within CardLayout
    private JComboBox<EduClass> assignmentClassComboBox;
    private JTable assignmentTable;
    private DefaultTableModel assignmentTableModel;
    private JScrollPane assignmentTableScrollPane;
    private JButton addAssignmentButton;
    private JButton editAssignmentButton;
    private JButton deleteAssignmentButton;
    private JPanel assignmentButtonPanel;
    private JPanel assignmentTopPanel;
    private JPanel comboPanel; // Panel for combobox label + combobox

    private JPanel placeholderPanel;
    private JLabel placeholderLabel;

    private CardLayout rightPanelLayout;
    private static final String GRADE_PANEL_CARD = "Grades";
    private static final String ASSIGNMENT_PANEL_CARD = "Assignments";
    private static final String PLACEHOLDER_CARD = "Placeholder";

    private static final DecimalFormat df = new DecimalFormat("#.00");
    private static final String[] SUBJECT_KEYS = {"Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD", "Nghệ thuật"};
    private static final String[] EDITABLE_SUBJECT_KEYS = {"Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD"};
    private static final String ART_KEY = "Nghệ thuật";
    private static final String CONDUCT_KEY = "Hạnh kiểm";
    private static final String[] TABLE_COLUMNS = {"STT", "Tên HS", "Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD", ART_KEY, "TB KHTN", "TB KHXH", "TB môn học", CONDUCT_KEY};
    private static final DateTimeFormatter ASSIGNMENT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


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
        cancelButton = new JButton("Hủy Bỏ");
        saveChangesButton = new JButton("Lưu Thay Đổi");
        saveChangesButton.setIcon(UIUtils.loadSVGIcon("/icons/save.svg", 20));
        exportButton = new JButton("Xuất Excel");
        exportButton.setIcon(UIUtils.loadSVGIcon("/icons/export.svg", 20));

        resultsRootIcon = UIUtils.loadSVGIcon("/icons/results.svg", 33);
        assignmentIcon = UIUtils.loadSVGIcon("/icons/assignment.svg", 33);

        editButton.setEnabled(false);
        saveChangesButton.setVisible(false);
        cancelButton.setVisible(false);
        exportButton.setEnabled(false);

        selectedClassLabel = new JLabel("Select a class from 'Results & reviews'", SwingConstants.CENTER);
        selectedClassLabel.setFont(selectedClassLabel.getFont().deriveFont(Font.BOLD));

        gradeTableModel = new DefaultTableModel(TABLE_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (isEditing && controller != null && controller.canCurrentUserEditGrades()) {
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
                if (columnIndex == 0 || columnIndex == 1) {
                    return String.class;
                }
                return Double.class;
            }
        };

        gradeTable = new JTable(gradeTableModel);
        gradeTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        gradeTable.setRowHeight(25);
        gradeTable.getTableHeader().setReorderingAllowed(false);
        gradeTable.setCellSelectionEnabled(true);
        gradeTable.setRowSelectionAllowed(false);
        gradeTable.setColumnSelectionAllowed(false);
        JTableHeader gradeHeader = gradeTable.getTableHeader();
        if (gradeHeader != null) {
            gradeHeader.setFont(gradeHeader.getFont().deriveFont(Font.BOLD));
            DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) gradeHeader.getDefaultRenderer();
            if (headerRenderer != null) {
                headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
            }
        }
        gradeTable.setShowGrid(true);
        gradeTable.setGridColor(Color.LIGHT_GRAY);
        setupTableEditorsAndRenderers();
        gradeTableScrollPane = new JScrollPane(gradeTable);
        gradeTableScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        gradeButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topOfGradePanel = new JPanel(new BorderLayout(10, 0));
        gradePanel = new JPanel(new BorderLayout(5, 10));

        assignmentClassComboBox = new JComboBox<>();
        assignmentClassComboBox.setPrototypeDisplayValue(new EduClass(0, "Longest Class Name Possible Here", null, null, 0, "", ""));

        assignmentTableModel = new DefaultTableModel(new String[]{"ID", "Title", "Due Date", "Description"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        assignmentTable = new JTable(assignmentTableModel);
        assignmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        assignmentTable.setAutoCreateRowSorter(true);
        assignmentTable.getTableHeader().setReorderingAllowed(false);
        assignmentTableScrollPane = new JScrollPane(assignmentTable);

        addAssignmentButton = new JButton("Add");
        addAssignmentButton.setIcon(UIUtils.loadSVGIcon("/icons/add.svg", 20));
        editAssignmentButton = new JButton("Edit");
        editAssignmentButton.setIcon(UIUtils.loadSVGIcon("/icons/edit.svg", 20));
        deleteAssignmentButton = new JButton("Delete");
        deleteAssignmentButton.setIcon(UIUtils.loadSVGIcon("/icons/delete.svg", 20));

        addAssignmentButton.setEnabled(false);
        editAssignmentButton.setEnabled(false);
        deleteAssignmentButton.setEnabled(false);

        assignmentButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        comboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        assignmentTopPanel = new JPanel(new BorderLayout(10, 5));
        assignmentManagementPanel = new JPanel(new BorderLayout(5, 10));

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

        rightPanelLayout = new CardLayout();
        rightPanel = new JPanel(rightPanelLayout);

        placeholderPanel = new JPanel(new BorderLayout());
        placeholderLabel = new JLabel("Select an item from the left.", SwingConstants.CENTER);
        placeholderLabel.setName("placeholderLabel");
        placeholderLabel.setForeground(Color.GRAY);
        placeholderLabel.setFont(placeholderLabel.getFont().deriveFont(14f));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.20);
        splitPane.setDividerSize(8);
    }

    private void setupTableEditorsAndRenderers() {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer rightNumberRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                // Gọi phương thức cha để lấy label cơ bản
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.RIGHT); // Căn phải

                Number numberToFormat = null; // Biến để lưu giá trị số sau khi xử lý

                // Xử lý các kiểu dữ liệu khác nhau cho value
                if (value instanceof Number) {
                    // Nếu đã là Number, dùng luôn
                    numberToFormat = (Number) value;
                } else if (value instanceof String) {
                    // Nếu là String, cố gắng chuyển đổi (parse) thành Double
                    String stringValue = ((String) value).trim();
                    if (!stringValue.isEmpty()) {
                        try {
                            // Thử parse thành Double
                            numberToFormat = Double.parseDouble(stringValue);
                        } catch (NumberFormatException e) {
                            // Nếu parse lỗi, numberToFormat vẫn là null
                            // Có thể log lỗi nếu muốn:
                            // System.err.println("Renderer parse error: " + e.getMessage() + " for value: " + value);
                        }
                    }
                    // Nếu stringValue rỗng, numberToFormat vẫn là null
                }
                // Các kiểu dữ liệu khác (hoặc null) sẽ khiến numberToFormat là null

                // Định dạng và hiển thị
                if (numberToFormat != null) {
                    // Nếu có số hợp lệ, định dạng nó
                    label.setText(df.format(numberToFormat.doubleValue()));
                } else {
                    // Nếu không có số hợp lệ (null, String rỗng, String lỗi, kiểu khác)
                    // Hiển thị giá trị gốc dưới dạng String (hoặc rỗng nếu null)
                    // Điều này ngăn ô bị trống một cách không cần thiết
                    label.setText(value != null ? value.toString() : "");
                }

                return label;
            }
        };

        DefaultTableCellRenderer boldRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setFont(c.getFont().deriveFont(Font.BOLD));
                return c;
            }
        };

        DefaultTableCellRenderer enumRenderer = new DefaultTableCellRenderer();
        enumRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        for (int i = 0; i < gradeTable.getColumnCount(); i++) {
            TableColumn column = gradeTable.getColumnModel().getColumn(i);
            String colName = gradeTable.getColumnName(i);

            if (colName.equals("Tên HS")) {
                column.setCellRenderer(boldRenderer);
            } else if (colName.equals("STT")) {
                column.setCellRenderer(centerRenderer);
            } else if (colName.equals(ART_KEY) || colName.equals(CONDUCT_KEY)) {
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
            gradeTable.getColumnModel().getColumn(getColumnIndex("STT")).setMinWidth(30);
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
            System.err.println("Error setting grade table column widths: " + e.getMessage());
        }
    }

    private void setupLayout() {
        leftPanel.add(treeScrollPane, BorderLayout.CENTER);

        gradeButtonPanel.add(editButton);
        gradeButtonPanel.add(cancelButton);
        gradeButtonPanel.add(saveChangesButton);
        gradeButtonPanel.add(exportButton);

        topOfGradePanel.add(selectedClassLabel, BorderLayout.CENTER);
        topOfGradePanel.add(gradeButtonPanel, BorderLayout.EAST);
        topOfGradePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        gradePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        gradePanel.add(topOfGradePanel, BorderLayout.NORTH);
        gradePanel.add(gradeTableScrollPane, BorderLayout.CENTER);

        assignmentButtonPanel.add(addAssignmentButton);
        assignmentButtonPanel.add(editAssignmentButton);
        assignmentButtonPanel.add(deleteAssignmentButton);

        comboPanel.add(new JLabel("Manage Assignments for Class:"));
        comboPanel.add(assignmentClassComboBox);
        assignmentTopPanel.add(comboPanel, BorderLayout.WEST);
        assignmentTopPanel.add(assignmentButtonPanel, BorderLayout.EAST);

        assignmentManagementPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        assignmentManagementPanel.add(assignmentTopPanel, BorderLayout.NORTH);
        assignmentManagementPanel.add(assignmentTableScrollPane, BorderLayout.CENTER);

        placeholderPanel.add(placeholderLabel, BorderLayout.CENTER);

        rightPanel.add(placeholderPanel, PLACEHOLDER_CARD);
        rightPanel.add(gradePanel, GRADE_PANEL_CARD);
        rightPanel.add(assignmentManagementPanel, ASSIGNMENT_PANEL_CARD);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);

        rightPanelLayout.show(rightPanel, PLACEHOLDER_CARD);
    }

    private void setupActions() {
        classTree.addTreeSelectionListener(e -> {
            if (e.isAddedPath()) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) classTree.getLastSelectedPathComponent();
                handleTreeNodeSelection(selectedNode);
            }
        });

        gradeTableModel.addTableModelListener(e -> {
            // Chỉ xử lý sự kiện UPDATE do người dùng chỉnh sửa gây ra
            if (e.getType() == TableModelEvent.UPDATE && isEditing) {
                int row = e.getFirstRow();
                int column = e.getColumn();

                // Kiểm tra tính hợp lệ của chỉ số và controller
                if (controller != null && row >= 0 && column >= 0 && row < gradeTableModel.getRowCount()) {
                    String columnName = gradeTableModel.getColumnName(column);

                    // Xác định xem cột có phải là cột dữ liệu có thể chỉnh sửa không
                    boolean isEditableDataColumn = false;
                    for (String key : EDITABLE_SUBJECT_KEYS) {
                        if (key.equals(columnName)) {
                            isEditableDataColumn = true;
                            break;
                        }
                    }
                    if (!isEditableDataColumn) {
                        isEditableDataColumn = ART_KEY.equals(columnName) || CONDUCT_KEY.equals(columnName);
                    }

                    // Nếu là cột dữ liệu có thể chỉnh sửa
                    if (isEditableDataColumn) {
                        Object newValueFromEditor = gradeTableModel.getValueAt(row, column);

                        System.out.println("Grade Table cell update event: row=" + row + ", col=" + column + ", name=" + columnName + ", value from editor=" + newValueFromEditor);

                        // Cập nhật dữ liệu thực tế trong controller
                        controller.updateRecordInMemory(row, columnName, newValueFromEditor);
                        AcademicRecord updatedRecord = controller.getAcademicRecordAt(row);

                        // Nếu cập nhật trong controller thành công
                        if (updatedRecord != null) {
                            markChangesPending(true); // Đánh dấu có thay đổi chưa lưu

                            // QUAN TRỌNG: Cập nhật các giá trị tính toán trong TableModel VÀ thông báo cho Table
                            try {
                                int khtnIndex = getColumnIndex("TB KHTN");
                                int khxhIndex = getColumnIndex("TB KHXH");
                                int tbMonHocIndex = getColumnIndex("TB môn học");

                                // Lấy cấu trúc dữ liệu nội bộ của DefaultTableModel (hơi không đẹp nhưng cần thiết)
                                Vector dataVector = gradeTableModel.getDataVector();
                                if (row < dataVector.size()) {
                                    Vector rowVector = (Vector) dataVector.elementAt(row);

                                    // Cập nhật giá trị KHTN trong model và thông báo
                                    if (khtnIndex != -1 && khtnIndex < rowVector.size()) {
                                        Double avgKHTN = updatedRecord.calculateAvgNaturalSciences();
                                        // Chỉ cập nhật nếu giá trị khác để tránh vòng lặp không cần thiết (mặc dù listener check type=UPDATE)
                                        if (!areEqual(rowVector.elementAt(khtnIndex), avgKHTN)) {
                                            rowVector.setElementAt(avgKHTN, khtnIndex);
                                            gradeTableModel.fireTableCellUpdated(row, khtnIndex); // Thông báo cho ô KHTN
                                        }
                                    }

                                    // Cập nhật giá trị KHXH trong model và thông báo
                                    if (khxhIndex != -1 && khxhIndex < rowVector.size()) {
                                        Double avgKHXH = updatedRecord.calculateAvgSocialSciences();
                                        if (!areEqual(rowVector.elementAt(khxhIndex), avgKHXH)) {
                                            rowVector.setElementAt(avgKHXH, khxhIndex);
                                            gradeTableModel.fireTableCellUpdated(row, khxhIndex); // Thông báo cho ô KHXH
                                        }
                                    }

                                    // Cập nhật giá trị TB Môn học trong model và thông báo
                                    if (tbMonHocIndex != -1 && tbMonHocIndex < rowVector.size()) {
                                        Double avgOverall = updatedRecord.calculateAvgOverallSubjects();
                                        if (!areEqual(rowVector.elementAt(tbMonHocIndex), avgOverall)) {
                                            rowVector.setElementAt(avgOverall, tbMonHocIndex);
                                            gradeTableModel.fireTableCellUpdated(row, tbMonHocIndex); // Thông báo cho ô TB Môn học
                                        }
                                    }
                                } else {
                                    System.err.println("Row index out of bounds for dataVector: " + row);
                                }

                            } catch (Exception calcEx) {
                                System.err.println("Error updating calculated grade cells visually in listener: " + calcEx.getMessage());
                                calcEx.printStackTrace(); // In stack trace để debug
                            }

                        } else {
                            // Xử lý trường hợp controller không cập nhật được (ví dụ: dữ liệu không hợp lệ)
                            System.err.println("controller.updateRecordInMemory failed for row " + row + ", column " + columnName + ". Value: " + newValueFromEditor);
                            // Optional: Có thể hiển thị lỗi cho người dùng hoặc hoàn tác thay đổi trong table model
                            // Ví dụ: tải lại dữ liệu gốc cho ô đó nếu có thể
                        }
                    }
                }
            }
        });


        editButton.addActionListener(e -> {
            setEditingMode(true);
            UIUtils.showInfoMessage(this, "Edit Mode", "You can now edit grades and conduct.");
        });

        cancelButton.addActionListener(e -> {
            if (hasPendingChanges) {
                if (!UIUtils.showConfirmDialog(this, "Discard Changes?", "Are you sure you want to discard all unsaved changes?")) {
                    return;
                }
            }
            setEditingMode(false);
            markChangesPending(false);
            if(controller != null && controller.getCurrentSelectedClassId() > 0){
                System.out.println("Cancelling edit, reloading grade data for class: " + controller.getCurrentSelectedClassId());
                controller.loadDataForClass(controller.getCurrentSelectedClassId());
            }
        });

        saveChangesButton.addActionListener(e -> {
            if (controller != null) {
                controller.saveAllChanges();
            }
        });

        exportButton.addActionListener(e -> {
            if (controller != null && mainController != null && controller.getCurrentSelectedClassId() > 0) {
                mainController.requestExcelExport(MainController.EXPORT_GRADES, controller.getCurrentSelectedClassId());
            } else {
                if(mainController == null) System.err.println("Export Error: MainController reference is null in EducationPanel.");
                TreePath selectionPath = classTree.getSelectionPath();
                boolean classNodeSelected = selectionPath != null &&
                        selectionPath.getLastPathComponent() instanceof DefaultMutableTreeNode &&
                        ((DefaultMutableTreeNode)selectionPath.getLastPathComponent()).getUserObject() instanceof EduClass;

                if (!classNodeSelected) {
                    UIUtils.showWarningMessage(this, "Cannot Export", "Please select a class under 'Results & reviews' first.");
                } else {
                    UIUtils.showWarningMessage(this, "Cannot Export", "Export function not available currently.");
                }
            }
        });

        assignmentClassComboBox.addActionListener(e -> {
            EduClass selectedClass = (EduClass) assignmentClassComboBox.getSelectedItem();
            if (controller != null && selectedClass != null) {
                controller.loadAssignmentsForClass(selectedClass.getClassId());
            } else if (controller != null) {
                displayAssignments(Collections.emptyList());
            }
            updateAssignmentButtonStates();
        });

        addAssignmentButton.addActionListener(e -> {
            EduClass selectedClass = getSelectedAssignmentClass();
            if (controller != null && selectedClass != null) {
                controller.handleAddAssignment(selectedClass);
            } else {
                UIUtils.showWarningMessage(this, "Action Required", "Please select a class first.");
            }
        });

        editAssignmentButton.addActionListener(e -> {
            int selectedRow = assignmentTable.getSelectedRow();
            if (selectedRow >= 0) {
                EduClass selectedClass = getSelectedAssignmentClass();
                if (selectedClass != null) {
                    int modelRow = assignmentTable.convertRowIndexToModel(selectedRow);
                    int assignmentId = (int) assignmentTableModel.getValueAt(modelRow, 0);
                    if (controller != null) {
                        controller.handleEditAssignment(selectedClass, assignmentId);
                    }
                } else {
                    UIUtils.showWarningMessage(this, "Action Required", "Please select a class first.");
                }
            } else {
                UIUtils.showWarningMessage(this, "Action Required", "Please select an assignment to edit.");
            }
        });

        deleteAssignmentButton.addActionListener(e -> {
            int selectedRow = assignmentTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = assignmentTable.convertRowIndexToModel(selectedRow);
                int assignmentId = (int) assignmentTableModel.getValueAt(modelRow, 0);
                String assignmentTitle = (String) assignmentTableModel.getValueAt(modelRow, 1);

                if (UIUtils.showConfirmDialog(this, "Confirm Deletion", "Are you sure you want to delete assignment '" + assignmentTitle + "'?")) {
                    if (controller != null) {
                        controller.handleDeleteAssignment(assignmentId);
                    }
                }
            } else {
                UIUtils.showWarningMessage(this, "Action Required", "Please select an assignment to delete.");
            }
        });

        assignmentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateAssignmentButtonStates();
            }
        });
    }

    private void handleTreeNodeSelection(DefaultMutableTreeNode selectedNode) {
        if (selectedNode == null) {
            showPlaceholderView("Please select an item from the left.");
            return;
        }

        Object userObject = selectedNode.getUserObject();

        if (isEditing && hasPendingChanges) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "You have unsaved changes in the grade editor. Discard changes and continue?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (choice == JOptionPane.NO_OPTION) {
                return;
            } else {
                setEditingMode(false);
                markChangesPending(false);
            }
        } else {
            setEditingMode(false);
            markChangesPending(false);
        }

        if (userObject instanceof String) {
            String nodeText = (String) userObject;
            if ("Results & reviews".equals(nodeText)) {
                showPlaceholderView("Select a class under 'Results & reviews' to view grades.");
                if (controller != null) controller.clearSelectedClass();
                editButton.setEnabled(false);
                exportButton.setEnabled(false);
            } else if ("Assignments".equals(nodeText)) {
                rightPanelLayout.show(rightPanel, ASSIGNMENT_PANEL_CARD);
                if (controller != null) {
                    List<EduClass> classes = controller.getClassesForCurrentUser();
                    populateAssignmentClassComboBox(classes);
                    if (assignmentClassComboBox.getItemCount() > 0) {
                        assignmentClassComboBox.setSelectedIndex(0);
                    } else {
                        displayAssignments(Collections.emptyList());
                    }
                }
                updateAssignmentButtonStates();
            }
        } else if (userObject instanceof EduClass) {
            rightPanelLayout.show(rightPanel, GRADE_PANEL_CARD);
            EduClass selectedClass = (EduClass) userObject;
            selectedClassLabel.setText("Grades for Class: " + selectedClass.getClassName());
            if (controller != null) {
                controller.loadDataForClass(selectedClass.getClassId());
                boolean canEditGrades = controller.canCurrentUserEditGrades();
                boolean hasData = gradeTableModel.getRowCount() > 0;
                editButton.setEnabled(canEditGrades && hasData);
                exportButton.setEnabled(hasData);
            }
        } else {
            showPlaceholderView("Selection not recognized.");
        }
    }

    private void populateAssignmentClassComboBox(List<EduClass> classes) {
        Object selectedItem = assignmentClassComboBox.getSelectedItem();

        assignmentClassComboBox.removeAllItems();
        if (classes != null && !classes.isEmpty()) {
            classes.sort(Comparator.comparing(EduClass::getClassName, String.CASE_INSENSITIVE_ORDER));
            for (EduClass eduClass : classes) {
                assignmentClassComboBox.addItem(eduClass);
            }
            if (selectedItem instanceof EduClass && classes.contains(selectedItem)) {
                assignmentClassComboBox.setSelectedItem(selectedItem);
            } else {
                assignmentClassComboBox.setSelectedIndex(0);
            }
        }
        assignmentClassComboBox.setEnabled(assignmentClassComboBox.getItemCount() > 0);
    }

    private void updateAssignmentButtonStates() {
        EduClass selectedClass = getSelectedAssignmentClass();
        boolean classSelected = (selectedClass != null);
        boolean assignmentSelected = (assignmentTable.getSelectedRow() != -1);
        boolean canManage = (controller != null && controller.canCurrentUserManageAssignments());

        addAssignmentButton.setEnabled(classSelected && canManage && !isEditing);
        editAssignmentButton.setEnabled(classSelected && assignmentSelected && canManage && !isEditing);
        deleteAssignmentButton.setEnabled(classSelected && assignmentSelected && canManage && !isEditing);
    }

    public void configureControlsForRole(Role userRole) {
        this.currentUserRole = userRole;
        System.out.println("EducationPanel: Configuring controls for role: " + userRole);

        if (splitPane == null || leftPanel == null || rightPanel == null || editButton == null ||
                cancelButton == null || saveChangesButton == null || exportButton == null ||
                addAssignmentButton == null || editAssignmentButton == null || deleteAssignmentButton == null ||
                classTree == null || treeModel == null || resultsNode == null || assignmentsNode == null ||
                rightPanelLayout == null || assignmentManagementPanel == null || gradePanel == null || placeholderPanel == null)
        {
            System.err.println("EducationPanel: Components not fully initialized in configureControlsForRole. Aborting configuration.");
            return;
        }

        boolean isPrivilegedUser = (userRole == Role.ADMIN || userRole == Role.TEACHER);

        leftPanel.setVisible(isPrivilegedUser);
        splitPane.setDividerSize(isPrivilegedUser ? 8 : 0);
        if (isPrivilegedUser) {
            splitPane.setLeftComponent(leftPanel);
            reloadClassTree();
            handleTreeNodeSelection((DefaultMutableTreeNode) treeModel.getRoot());

            topOfGradePanel.setVisible(true);
            assignmentTopPanel.setVisible(true);
            gradePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        } else if (userRole == Role.STUDENT) {
            leftPanel.setVisible(false);
            splitPane.setLeftComponent(null);
            splitPane.setDividerSize(0);

            rightPanelLayout.show(rightPanel, GRADE_PANEL_CARD);
            if (controller != null) {
                controller.loadDataForCurrentStudent();
            }
            topOfGradePanel.setVisible(false);
            gradePanel.setBorder(BorderFactory.createTitledBorder("Bảng điểm cá nhân"));

        } else {
            splitPane.setVisible(false);
            this.removeAll();
            this.add(new JLabel("Access Restricted.", SwingConstants.CENTER));
        }

        editButton.setEnabled(false);
        cancelButton.setVisible(false);
        saveChangesButton.setVisible(false);
        exportButton.setEnabled(false);
        addAssignmentButton.setEnabled(false);
        editAssignmentButton.setEnabled(false);
        deleteAssignmentButton.setEnabled(false);

        this.revalidate();
        this.repaint();
    }


    public void reloadClassTree() {
        if (controller != null && treeModel != null && resultsNode != null && classTree != null) {
            System.out.println("EducationPanel: Reloading class tree.");

            TreePath selectedPathBefore = classTree.getSelectionPath();
            Object selectedUserObjectBefore = null;
            if (selectedPathBefore != null && selectedPathBefore.getLastPathComponent() instanceof DefaultMutableTreeNode) {
                selectedUserObjectBefore = ((DefaultMutableTreeNode) selectedPathBefore.getLastPathComponent()).getUserObject();
            }

            resultsNode.removeAllChildren();
            List<EduClass> classes = controller.getClassesForCurrentUser();
            DefaultMutableTreeNode nodeToSelectAfter = null;

            if (classes != null) {
                classes.sort(Comparator.comparing(EduClass::getClassName, String.CASE_INSENSITIVE_ORDER));
                for (EduClass eduClass : classes) {
                    DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(eduClass);
                    resultsNode.add(classNode);
                    if (selectedUserObjectBefore instanceof EduClass && eduClass.getClassId() == ((EduClass)selectedUserObjectBefore).getClassId()) {
                        nodeToSelectAfter = classNode;
                    }
                }
            }

            treeModel.reload(resultsNode);
            classTree.expandPath(new TreePath(resultsNode.getPath()));
            classTree.expandPath(new TreePath(assignmentsNode.getPath()));

            if (nodeToSelectAfter != null) {
                TreePath pathToSelect = new TreePath(nodeToSelectAfter.getPath());
                classTree.setSelectionPath(pathToSelect);
                classTree.scrollPathToVisible(pathToSelect);
                handleTreeNodeSelection(nodeToSelectAfter);
            } else if (selectedUserObjectBefore instanceof String && "Assignments".equals(selectedUserObjectBefore)) {
                nodeToSelectAfter = assignmentsNode;
                TreePath pathToSelect = new TreePath(nodeToSelectAfter.getPath());
                classTree.setSelectionPath(pathToSelect);
                classTree.scrollPathToVisible(pathToSelect);
                handleTreeNodeSelection(nodeToSelectAfter);
            }
            else {
                nodeToSelectAfter = resultsNode;
                TreePath pathToSelect = new TreePath(nodeToSelectAfter.getPath());
                classTree.setSelectionPath(pathToSelect);
                handleTreeNodeSelection(nodeToSelectAfter);
            }
            if (controller != null) {
                populateAssignmentClassComboBox(classes);
            }
            System.out.println("EducationPanel: Class tree reloaded and view potentially updated.");
        }
    }

    public void updateTableData(List<Student> students, List<AcademicRecord> records) {
        gradeTableModel.setRowCount(0);
        if (students == null || records == null || students.size() != records.size()) {
            System.err.println("Error updating grade table: Mismatch or null student/record lists.");
            setEditingMode(false);
            markChangesPending(false);
            editButton.setEnabled(false);
            exportButton.setEnabled(false);
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
        setEditingMode(false);
        markChangesPending(false);
        boolean canEdit = (controller != null && controller.canCurrentUserEditGrades());
        boolean hasData = gradeTableModel.getRowCount() > 0;
        editButton.setEnabled(canEdit && hasData);
        exportButton.setEnabled(hasData);
    }

    public void updateTableDataForStudent(Student student, List<AcademicRecord> records) {
        gradeTableModel.setRowCount(0);
        if (student == null || records == null) {
            System.err.println("Error updating student grade table: Null student or records.");
            return;
        }

        if (!records.isEmpty()) {
            AcademicRecord record = records.get(0);
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
        } else {
            System.out.println("No academic records found for student: " + student.getFullName());
        }

        setEditingMode(false);
        markChangesPending(false);
        editButton.setVisible(false);
        cancelButton.setVisible(false);
        saveChangesButton.setVisible(false);
        exportButton.setVisible(false);
    }

    public void displayAssignments(List<Assignment> assignments) {
        assignmentTableModel.setRowCount(0);
        if (assignments != null) {
            for (Assignment assignment : assignments) {
                Vector<Object> row = new Vector<>();
                row.add(assignment.getAssignmentId());
                row.add(assignment.getTitle());
                row.add(assignment.getDueDate() != null ? assignment.getDueDate().format(ASSIGNMENT_DATE_FORMATTER) : "");
                row.add(assignment.getDescription());
                assignmentTableModel.addRow(row);
            }
        }
        updateAssignmentButtonStates();
    }


    public void updateCalculatedValues(int rowIndex, AcademicRecord record) {
        if (rowIndex >= 0 && rowIndex < gradeTableModel.getRowCount() && record != null) {
            try {
                    int khtnIndex = getColumnIndex("TB KHTN");
                    int khxhIndex = getColumnIndex("TB KHXH");
                    int tbMonHocIndex = getColumnIndex("TB môn học");

                    if (khtnIndex != -1) {
                        gradeTableModel.setValueAt(record.calculateAvgNaturalSciences(), rowIndex, khtnIndex);
                    }
                    if (khxhIndex != -1) {
                        gradeTableModel.setValueAt(record.calculateAvgSocialSciences(), rowIndex, khxhIndex);
                    }
                    if (tbMonHocIndex != -1) {
                        gradeTableModel.setValueAt(record.calculateAvgOverallSubjects(), rowIndex, tbMonHocIndex);
                    }
            } catch(ArrayIndexOutOfBoundsException e) {
                System.err.println("Error updating calculated grade values, column index likely invalid: " + e.getMessage());
            }
        }
    }

    public void markChangesPending(boolean pending) {
        this.hasPendingChanges = pending;
        setEditingMode(this.isEditing);
    }

    public void refreshTableCell(int rowIndex, String subjectKey){
        if(rowIndex >= 0 && rowIndex < gradeTableModel.getRowCount()){
            int columnIndex = getColumnIndex(subjectKey);
            if(columnIndex != -1){
                System.out.println("Requesting repaint for grade cell: row=" + rowIndex + ", col=" + columnIndex);
                gradeTableModel.fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }

    public int getColumnIndex(String columnName) {
        if (gradeTable == null || columnName == null) return -1;
        for (int i = 0; i < gradeTable.getColumnCount(); i++) {
            if (columnName.equals(gradeTable.getColumnName(i))) {
                return i;
            }
        }
        return -1;
    }

    public DefaultTableModel getGradeTableModel() {
        return gradeTableModel;
    }

    private void showPlaceholderView(String message) {
        if (placeholderLabel != null) {
            placeholderLabel.setText("<html><center><i>" + message + "</i></center></html>");
        }
        if (rightPanelLayout != null && rightPanel != null) {
            rightPanelLayout.show(rightPanel, PLACEHOLDER_CARD);
        }
        editButton.setEnabled(false);
        exportButton.setEnabled(false);
        addAssignmentButton.setEnabled(false);
        editAssignmentButton.setEnabled(false);
        deleteAssignmentButton.setEnabled(false);
        setEditingMode(false);
        markChangesPending(false);
    }

    private void showGradeTableView() {
        if (rightPanelLayout != null && rightPanel != null) {
            rightPanelLayout.show(rightPanel, GRADE_PANEL_CARD);
        }
    }

    private class EduClassTreeCellRenderer extends DefaultTreeCellRenderer {
        private final Icon defaultClosedIcon = UIManager.getIcon("Tree.closedIcon");
        private final Icon defaultOpenIcon = UIManager.getIcon("Tree.openIcon");
        private final Icon defaultLeafIcon = UIManager.getIcon("Tree.leafIcon");
        private final Icon specificResultsIcon;
        private final Icon specificAssignmentIcon;


        public EduClassTreeCellRenderer(Icon resultsIcon, Icon assignmentIcon) {
            this.specificResultsIcon = resultsIcon;
            this.specificAssignmentIcon = assignmentIcon;
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
                        label.setIcon(specificResultsIcon != null ? specificResultsIcon : (expanded ? defaultOpenIcon : defaultClosedIcon));
                    } else if ("Assignments".equals(nodeText)) {
                        label.setIcon(specificAssignmentIcon != null ? specificAssignmentIcon : (expanded ? defaultOpenIcon : defaultClosedIcon));
                    } else {
                        label.setIcon(expanded ? defaultOpenIcon : defaultClosedIcon);
                    }
                } else if (userObject instanceof EduClass) {
                    EduClass eduClass = (EduClass) userObject;
                    label.setText(eduClass.getClassName());
                    label.setIcon(defaultLeafIcon);
                } else {
                    label.setText(userObject == null ? "" : userObject.toString());
                    label.setIcon(defaultLeafIcon);
                }
            } else {
                label.setText(value == null ? "" : value.toString());
            }
            return label;
        }
    }

    private void setEditingMode(boolean editing) {
        this.isEditing = editing;
        boolean canEditGrades = (controller != null && controller.canCurrentUserEditGrades());
        boolean hasGradeData = (gradeTableModel != null && gradeTableModel.getRowCount() > 0);
        boolean hasAssignmentSelection = (assignmentTable != null && assignmentTable.getSelectedRow() != -1);
        boolean canManageAssignments = (controller != null && controller.canCurrentUserManageAssignments());
        EduClass selectedAssignmentClass = getSelectedAssignmentClass();
        boolean assignmentClassSelected = (selectedAssignmentClass != null);


        if (editButton != null) {
            editButton.setVisible(!editing);
            editButton.setEnabled(!editing && canEditGrades && hasGradeData);
        }
        if (cancelButton != null) {
            cancelButton.setVisible(editing);
        }
        if (saveChangesButton != null) {
            saveChangesButton.setVisible(editing);
            saveChangesButton.setEnabled(editing && canEditGrades && hasPendingChanges);
        }

        if (assignmentClassComboBox != null) {
            assignmentClassComboBox.setEnabled(!editing);
        }
        if (addAssignmentButton != null) {
            addAssignmentButton.setEnabled(!editing && assignmentClassSelected && canManageAssignments);
        }
        if (editAssignmentButton != null) {
            editAssignmentButton.setEnabled(!editing && assignmentClassSelected && hasAssignmentSelection && canManageAssignments);
        }
        if (deleteAssignmentButton != null) {
            deleteAssignmentButton.setEnabled(!editing && assignmentClassSelected && hasAssignmentSelection && canManageAssignments);
        }

        if (gradeTable != null) {
            gradeTable.repaint();
        }
        System.out.println("Grade editing mode set to: " + editing);
    }

    public void updateSpecificCellValue(int rowIndex, String subjectKey, Object value) {
        int columnIndex = getColumnIndex(subjectKey);
        if (rowIndex >= 0 && rowIndex < gradeTableModel.getRowCount() && columnIndex != -1) {
                System.out.println("Updating Grade TableModel cell: row=" + rowIndex + ", col=" + columnIndex + ", key=" + subjectKey + ", value=" + value);
                gradeTableModel.setValueAt(value, rowIndex, columnIndex);
        } else {
            System.err.println("Error updating specific grade cell value: Invalid row/column index or key not found. Row: " + rowIndex + ", Key: " + subjectKey);
        }
    }

    public EduClass getSelectedAssignmentClass() {
        if (assignmentClassComboBox != null) {
            return (EduClass) assignmentClassComboBox.getSelectedItem();
        }
        return null;
    }
    private boolean areEqual(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) return true;
        if (obj1 == null || obj2 == null) return false;
        // Thêm các kiểu so sánh khác nếu cần (ví dụ: Enum)
        if (obj1 instanceof Double && obj2 instanceof Double) {
            // So sánh Double với sai số nhỏ để tránh vấn đề dấu phẩy động
            return Math.abs(((Double)obj1) - ((Double)obj2)) < 0.001;
        }
        return obj1.equals(obj2);
    }

}