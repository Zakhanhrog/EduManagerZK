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

import javax.swing.border.TitledBorder;
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
import java.util.*;
import java.util.List;

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

    private javax.swing.Timer assignmentStatusTimer;
    private final int STATUS_COLUMN_INDEX = 4;
    private List<Assignment> currentlyDisplayedAssignmentsInPanel;

    private JPanel studentInfoPanel; // Panel để chứa các label thông tin
    private JLabel studentNameLabel;
    private JLabel studentIdLabel;
    private JLabel studentDobLabel;
    private JLabel studentGenderLabel;
    private JLabel studentClassLabel; // Sẽ lấy từ EduClass nếu có
    private JLabel studentPhoneLabel;
    private JLabel studentEmailLabel;
    private JLabel studentParentLabel;
    private TableColumn sttColumnHolder;
    private TableColumn studentNameColumnHolder;
    private final int STT_ORIGINAL_INDEX = 0;
    private final int STUDENT_NAME_ORIGINAL_INDEX = 1;

    private void initializeAssignmentStatusUpdater() {
        assignmentStatusTimer = new javax.swing.Timer(60000, e -> checkAndUpdateAssignmentStatuses());
        assignmentStatusTimer.setInitialDelay(10000);
    }

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

        studentInfoPanel = new JPanel();
        studentInfoPanel.setLayout(new BorderLayout());
        studentInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0)); // Khoảng cách 10px ở trên
        studentInfoPanel.setVisible(false);

        studentNameLabel = new JLabel("Họ và tên: ");
        studentIdLabel = new JLabel("Mã HS: ");
        studentDobLabel = new JLabel("Ngày sinh: ");
        studentGenderLabel = new JLabel("Giới tính: ");
        studentClassLabel = new JLabel("Lớp: ");
        studentPhoneLabel = new JLabel("Điện thoại: ");
        studentEmailLabel = new JLabel("Email: ");
        studentParentLabel = new JLabel("Phụ huynh: ");

        JPanel infoGridPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        infoGridPanel.setBorder(BorderFactory.createTitledBorder("Thông tin cá nhân"));
        infoGridPanel.add(new JLabel("<html><b>Họ và tên:</b></html>")); infoGridPanel.add(studentNameLabel);
        infoGridPanel.add(new JLabel("<html><b>Mã HS:</b></html>")); infoGridPanel.add(studentIdLabel);
        infoGridPanel.add(new JLabel("<html><b>Ngày sinh:</b></html>")); infoGridPanel.add(studentDobLabel);
        infoGridPanel.add(new JLabel("<html><b>Giới tính:</b></html>")); infoGridPanel.add(studentGenderLabel);
        infoGridPanel.add(new JLabel("<html><b>Lớp hiện tại:</b></html>")); infoGridPanel.add(studentClassLabel);
        infoGridPanel.add(new JLabel("<html><b>Điện thoại:</b></html>")); infoGridPanel.add(studentPhoneLabel);
        infoGridPanel.add(new JLabel("<html><b>Email:</b></html>")); infoGridPanel.add(studentEmailLabel);
        infoGridPanel.add(new JLabel("<html><b>Phụ huynh:</b></html>")); infoGridPanel.add(studentParentLabel);

        studentInfoPanel.add(infoGridPanel, BorderLayout.CENTER);

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
        gradeTable.setOpaque(true);
        gradeTableScrollPane.setOpaque(true);
        gradeTableScrollPane.getViewport().setOpaque(true);

        gradeButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topOfGradePanel = new JPanel(new BorderLayout(10, 0));
        gradePanel = new JPanel(new BorderLayout(5, 10));

        assignmentClassComboBox = new JComboBox<>();
        assignmentClassComboBox.setPrototypeDisplayValue(new EduClass(0, "Longest Class Name Possible Here", null, null, 0, "", ""));

        assignmentTableModel = new DefaultTableModel(
                new String[]{"ID", "Title", "Deadline", "Description", "Status"},
                0
        ) {
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

                Number numberToFormat = null;

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
                }
                if (numberToFormat != null) {
                    // Nếu có số hợp lệ, định dạng nó
                    label.setText(df.format(numberToFormat.doubleValue()));
                } else {
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
            // Quan trọng: Dùng getColumnIndexFromView để lấy index hiện tại trong view
            int sttIndex = getColumnIndexFromView("STT");
            if(sttIndex != -1) {
                gradeTable.getColumnModel().getColumn(sttIndex).setMaxWidth(40);
                gradeTable.getColumnModel().getColumn(sttIndex).setMinWidth(30);
                gradeTable.getColumnModel().getColumn(sttIndex).setPreferredWidth(35);
            }
            int nameIndex = getColumnIndexFromView("Tên HS");
            if(nameIndex != -1) {
                gradeTable.getColumnModel().getColumn(nameIndex).setMinWidth(100);
                gradeTable.getColumnModel().getColumn(nameIndex).setPreferredWidth(180);
            }
            int artIndex = getColumnIndexFromView(ART_KEY);
            if(artIndex != -1) gradeTable.getColumnModel().getColumn(artIndex).setMinWidth(100);
            int conductIndex = getColumnIndexFromView(CONDUCT_KEY);
            if(conductIndex != -1) gradeTable.getColumnModel().getColumn(conductIndex).setMinWidth(100);
            int khtnIndex = getColumnIndexFromView("TB KHTN");
            if(khtnIndex != -1) gradeTable.getColumnModel().getColumn(khtnIndex).setPreferredWidth(80);
            int khxhIndex = getColumnIndexFromView("TB KHXH");
            if(khxhIndex != -1) gradeTable.getColumnModel().getColumn(khxhIndex).setPreferredWidth(80);
            int tbMonIndex = getColumnIndexFromView("TB môn học");
            if(tbMonIndex != -1) gradeTable.getColumnModel().getColumn(tbMonIndex).setPreferredWidth(90);

            for(String key : EDITABLE_SUBJECT_KEYS) {
                int colIdx = getColumnIndexFromView(key);
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
        JPanel centerContentPanel = new JPanel(new BorderLayout(5, 10));
        centerContentPanel.add(studentInfoPanel, BorderLayout.NORTH);
        centerContentPanel.add(gradeTableScrollPane, BorderLayout.CENTER);

        gradePanel.add(centerContentPanel, BorderLayout.CENTER);

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

            if (("Info & Results".equals(nodeText) && currentUserRole == Role.STUDENT) ||
                    ("Results & reviews".equals(nodeText) && (currentUserRole == Role.ADMIN || currentUserRole == Role.TEACHER)))
            {
                if (currentUserRole == Role.STUDENT) {
                    rightPanelLayout.show(rightPanel, GRADE_PANEL_CARD);
                    if (controller != null) {
                        controller.loadDataForCurrentStudent(); // Tải điểm của học sinh
                    }
                    editButton.setEnabled(false); // Học sinh không được sửa
                    exportButton.setEnabled(false); // Học sinh không được xuất (trừ khi có yêu cầu khác)
                } else {
                    // ADMIN/TEACHER: Hiển thị placeholder khi chọn node gốc "Results & reviews"
                    showPlaceholderView("Select a class under 'Results & reviews' to view grades.");
                    if (controller != null) controller.clearSelectedClass();
                    editButton.setEnabled(false);
                    exportButton.setEnabled(false);
                }
            } else if ("Assignments".equals(nodeText)) {
                // Xử lý khi chọn node Assignments cho TẤT CẢ vai trò có thể thấy nó
                rightPanelLayout.show(rightPanel, ASSIGNMENT_PANEL_CARD);
                if (controller != null) {
                    if (currentUserRole == Role.ADMIN || currentUserRole == Role.TEACHER) {
                        // Load combobox và danh sách bài tập cho lớp đầu tiên (nếu có)
                        List<EduClass> classes = controller.getClassesForCurrentUser();
                        populateAssignmentClassComboBox(classes); // Đã có sẵn
                        if (assignmentClassComboBox.getItemCount() > 0) {
                            assignmentClassComboBox.setSelectedIndex(0); // Tự động trigger loadAssignmentsForClass
                        } else {
                            displayAssignments(Collections.emptyList());
                        }
                        updateAssignmentButtonStates(); // Cập nhật nút cho Admin/Teacher
                    } else if (currentUserRole == Role.STUDENT) {
                        // STUDENT: Load bài tập cho lớp của học sinh
                        assignmentClassComboBox.setVisible(false); // Ẩn combobox chọn lớp
                        assignmentButtonPanel.setVisible(false); // Ẩn nút thêm/sửa/xóa
                        comboPanel.setVisible(false); // Ẩn label và combobox panel
                        assignmentTopPanel.setVisible(false); //Ẩn toàn bộ phần top của assignment panel

                        // Yêu cầu controller tải bài tập cho học sinh hiện tại
                        controller.loadAssignmentsForStudent(); // <<< Cần tạo phương thức này trong Controller
                    }
                }
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
        int selectedRowInView = assignmentTable.getSelectedRow();
        boolean assignmentSelected = (assignmentTable.getSelectedRow() != -1);
        boolean canManage = (controller != null && controller.canCurrentUserManageAssignments());
        boolean isSelectedAssignmentOverdue = false;
        if (assignmentSelected && controller != null) {
            int modelRow = assignmentTable.convertRowIndexToModel(selectedRowInView);
            // Đảm bảo modelRow hợp lệ trước khi truy cập
            if (modelRow >= 0 && modelRow < assignmentTableModel.getRowCount()) {
                int assignmentId = (int) assignmentTableModel.getValueAt(modelRow, 0); // Lấy ID từ cột 0
                isSelectedAssignmentOverdue = controller.isAssignmentOverdue(assignmentId); // Gọi controller
            } else {
                System.err.println("updateAssignmentButtonStates: Invalid modelRow " + modelRow);
                assignmentSelected = false; // Coi như không có gì được chọn nếu modelRow không hợp lệ
            }
        }

        addAssignmentButton.setEnabled(classSelected && canManage && !isEditing);
        editAssignmentButton.setEnabled(classSelected && assignmentSelected && canManage && !isEditing);
        deleteAssignmentButton.setEnabled(classSelected && assignmentSelected && canManage && !isEditing);
    }

    // Trong lớp: com.eduzk.view.panels.EducationPanel

    public void configureControlsForRole(Role userRole) {
        this.currentUserRole = userRole;
        System.out.println("EducationPanel: Configuring controls for role: " + userRole);

        // --- B1: Kiểm tra các component cốt lõi đã được khởi tạo chưa ---
        if (splitPane == null || leftPanel == null || rightPanel == null ||
                gradeTable == null || gradePanel == null || gradeTableScrollPane == null ||
                studentInfoPanel == null || topOfGradePanel == null || assignmentTopPanel == null ||
                controller == null || treeModel == null || mainRootNode == null) { // Thêm các kiểm tra cần thiết

            System.err.println("EducationPanel: Core components or controller not fully initialized in configureControlsForRole. Aborting configuration.");
            // Hiển thị thông báo lỗi hoặc trạng thái không khả dụng
            this.removeAll(); // Xóa hết component con hiện tại
            this.add(new JLabel("Error: Education Panel components failed to initialize.", SwingConstants.CENTER));
            this.revalidate();
            this.repaint();
            return; // Không thực hiện tiếp nếu thiếu component
        }
        // --- Kết thúc B1 ---

        boolean isPrivilegedUser = (userRole == Role.ADMIN || userRole == Role.TEACHER);

        // --- B2: Cấu hình hiển thị chung cho SplitPane và Left Panel (luôn hiển thị) ---
        leftPanel.setVisible(true);
        if (splitPane.getLeftComponent() != leftPanel) {
            splitPane.setLeftComponent(leftPanel);
        }
        splitPane.setDividerSize(8);
        splitPane.setVisible(true);
        // Đảm bảo splitPane là component gốc của EducationPanel
        if (this.getComponentCount() == 0 || !(this.getComponent(0) instanceof JSplitPane)) {
            this.removeAll();
            this.add(splitPane, BorderLayout.CENTER);
        }
        // --- Kết thúc B2 ---

        // --- B3: Lấy hoặc tạo panel trung tâm trong gradePanel ---
        // Panel này sẽ chứa studentInfoPanel (cho Student) và gradeTableScrollPane
        JPanel centerContentPanel;
        Component centerComponent = null;
        // gradePanel dùng BorderLayout, component trung tâm thường ở index 1 (sau topOfGradePanel ở NORTH)
        if (gradePanel.getComponentCount() > 1) {
            centerComponent = gradePanel.getComponent(1);
        }

        if (centerComponent instanceof JPanel) {
            centerContentPanel = (JPanel) centerComponent;
            // Đảm bảo nó dùng BorderLayout để xếp chồng info và table
            if (!(centerContentPanel.getLayout() instanceof BorderLayout)) {
                centerContentPanel.setLayout(new BorderLayout(5, 10));
            }
        } else {
            // Nếu không có hoặc không đúng loại, tạo mới và thêm vào gradePanel
            System.out.println("Creating/Re-adding centerContentPanel inside gradePanel.");
            if (centerComponent != null) {
                gradePanel.remove(centerComponent); // Xóa component cũ nếu có
            }
            centerContentPanel = new JPanel(new BorderLayout(5, 10));
            gradePanel.add(centerContentPanel, BorderLayout.CENTER); // Thêm vào vị trí trung tâm
        }
        centerContentPanel.removeAll(); // Luôn xóa nội dung cũ của panel trung tâm
        // --- Kết thúc B3 ---

        TableColumnModel columnModel = gradeTable.getColumnModel();

        // --- B4: Cấu hình chi tiết dựa trên vai trò ---
        if (isPrivilegedUser) { // ---- Cấu hình cho ADMIN / TEACHER ----
            reloadClassTree();          // Tải lại cây thư mục đầy đủ
            handleTreeNodeSelection(null); // Chọn node mặc định (placeholder)
            studentInfoPanel.setVisible(false); // Ẩn panel thông tin cá nhân

            // -- Cấu hình lại bảng điểm về trạng thái mặc định --
            gradeTable.setTableHeader(gradeTable.getTableHeader()); // Đảm bảo header hiển thị
            gradeTable.setShowGrid(true);                           // Hiển thị đường kẻ ô
            gradeTableScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY)); // Border mặc định

            gradeTable.setOpaque(true);
            gradeTableScrollPane.setOpaque(true);
            gradeTableScrollPane.getViewport().setOpaque(true);
            // -- Thêm lại các cột đã ẩn (STT, Tên HS) nếu cần --
            boolean columnsRestored = false;
            // Thêm lại cột Tên HS nếu chưa có và đã lưu
            if (studentNameColumnHolder != null && getColumnIndexFromView("Tên HS") == -1) {
                try {
                    columnModel.addColumn(studentNameColumnHolder);
                    columnsRestored = true;
                } catch (Exception e) {System.err.println("Error re-adding 'Tên HS' column: " + e);}
            }
            // Thêm lại cột STT nếu chưa có và đã lưu
            if (sttColumnHolder != null && getColumnIndexFromView("STT") == -1) {
                try {
                    columnModel.addColumn(sttColumnHolder);
                    columnsRestored = true;
                } catch (Exception e) {System.err.println("Error re-adding 'STT' column: " + e);}
            }
            // Di chuyển các cột về đúng vị trí nếu đã thêm lại
            if (columnsRestored) {
                try {
                    int currentSttIdx = getColumnIndexFromView("STT");
                    if(currentSttIdx != -1 && currentSttIdx != STT_ORIGINAL_INDEX) {
                        columnModel.moveColumn(currentSttIdx, STT_ORIGINAL_INDEX);
                    }
                    int currentNameIdx = getColumnIndexFromView("Tên HS");
                    // Index của Tên HS phải được kiểm tra sau khi STT đã về đúng vị trí (nếu có)
                    if(currentNameIdx != -1 && currentNameIdx != STUDENT_NAME_ORIGINAL_INDEX) {
                        columnModel.moveColumn(currentNameIdx, STUDENT_NAME_ORIGINAL_INDEX);
                    }
                    System.out.println("Columns restored and reordered for Admin/Teacher view.");
                    setColumnWidths(); // Đặt lại độ rộng các cột
                } catch (Exception e) {
                    System.err.println("Error moving restored columns: " + e.getMessage());
                }
                // Xóa tham chiếu sau khi thêm lại thành công
                sttColumnHolder = null;
                studentNameColumnHolder = null;
            }
            // -- Kết thúc thêm lại cột --

            // Thêm bảng điểm vào panel trung tâm
            centerContentPanel.add(gradeTableScrollPane, BorderLayout.CENTER);

            // Hiển thị các nút và panel điều khiển của Admin/Teacher
            topOfGradePanel.setVisible(true);
            assignmentTopPanel.setVisible(true);
            gradePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Padding mặc định


        } else if (userRole == Role.STUDENT) { // ---- Cấu hình cho STUDENT ----
            reloadClassTree();          // Tải lại cây (chỉ có node gốc)
            handleTreeNodeSelection(null); // Chọn node "Info & Results", sẽ tải data HS

            // -- Hiển thị panel thông tin cá nhân --
            studentInfoPanel.setVisible(true);
            centerContentPanel.add(studentInfoPanel, BorderLayout.NORTH); // Đặt thông tin lên trên

            // -- Cấu hình bảng điểm cho Student --
            gradeTable.setTableHeader(gradeTable.getTableHeader()); // VẪN GIỮ HEADER
            gradeTable.setShowGrid(false);                           // KHÔNG hiển thị lưới
            gradeTableScrollPane.setBorder(BorderFactory.createTitledBorder("Kết quả học tập")); // Đặt tiêu đề

            gradeTable.setOpaque(false);                       // Làm bảng trong suốt
            gradeTableScrollPane.setOpaque(false);            // Làm scroll pane trong suốt
            gradeTableScrollPane.getViewport().setOpaque(false);

            // -- Ẩn cột STT và Tên HS --
            // Chỉ ẩn nếu cột chưa được lưu trữ (tránh lỗi khi gọi lại)
            if (sttColumnHolder == null) {
                int sttViewIndex = getColumnIndexFromView("STT");
                if (sttViewIndex != -1) {
                    try {
                        sttColumnHolder = columnModel.getColumn(sttViewIndex);
                        columnModel.removeColumn(sttColumnHolder);
                        System.out.println("Removed 'STT' column for student view.");
                    } catch (Exception e) { System.err.println("Error removing STT column: " + e); sttColumnHolder = null; }
                }
            }
            if (studentNameColumnHolder == null) {
                int studentNameViewIndex = getColumnIndexFromView("Tên HS"); // Lấy index Tên HS *sau khi* STT có thể đã bị xóa
                if (studentNameViewIndex != -1) {
                    try {
                        studentNameColumnHolder = columnModel.getColumn(studentNameViewIndex);
                        columnModel.removeColumn(studentNameColumnHolder);
                        System.out.println("Removed 'Tên HS' column for student view.");
                    } catch (Exception e) { System.err.println("Error removing Tên HS column: " + e); studentNameColumnHolder = null; }
                }
            }
            // -- Kết thúc ẩn cột --

            // Thêm bảng điểm đã tùy chỉnh vào panel trung tâm
            centerContentPanel.add(gradeTableScrollPane, BorderLayout.CENTER);

            // Ẩn các nút và panel điều khiển của Admin/Teacher
            topOfGradePanel.setVisible(false);
            assignmentTopPanel.setVisible(false);
            gradePanel.setBorder(null); // Bỏ border của gradePanel

        } else { // ---- KHÔNG CÓ QUYỀN ----
            splitPane.setVisible(false);
            this.removeAll();
            this.add(new JLabel("Access Restricted.", SwingConstants.CENTER));
            studentInfoPanel.setVisible(false); // Đảm bảo panel info cũng bị ẩn
        }
        // --- Kết thúc B4 ---

        // --- B5: Reset trạng thái các nút chung ---
        editButton.setEnabled(false);
        cancelButton.setVisible(false);
        saveChangesButton.setVisible(false);
        exportButton.setEnabled(false);
        addAssignmentButton.setEnabled(false);
        editAssignmentButton.setEnabled(false);
        deleteAssignmentButton.setEnabled(false);
        // --- Kết thúc B5 ---

        // --- B6: Cập nhật giao diện ---
        centerContentPanel.revalidate();
        centerContentPanel.repaint();
        gradePanel.revalidate();
        gradePanel.repaint();
        this.revalidate();
        this.repaint();
        // --- Kết thúc B6 ---
    } // <-- Kết thúc phương thức configureControlsForRole


    public void reloadClassTree() {
        if (controller != null && treeModel != null && resultsNode != null && classTree != null) {
            System.out.println("EducationPanel: Reloading class tree.");

            TreePath selectedPathBefore = classTree.getSelectionPath();
            Object selectedUserObjectBefore = null;
            if (selectedPathBefore != null && selectedPathBefore.getLastPathComponent() instanceof DefaultMutableTreeNode) {
                selectedUserObjectBefore = ((DefaultMutableTreeNode) selectedPathBefore.getLastPathComponent()).getUserObject();
            }

            mainRootNode.removeAllChildren(); // Xóa cả results và assignments cũ

            // Tạo node Results dựa trên vai trò
            String resultsNodeText = (currentUserRole == Role.STUDENT) ? "Info & Results" : "Results & reviews";
            resultsNode = new DefaultMutableTreeNode(resultsNodeText);
            mainRootNode.add(resultsNode);

            // Tạo node Assignments (tên giữ nguyên)
            assignmentsNode = new DefaultMutableTreeNode("Assignments");
            mainRootNode.add(assignmentsNode);

            DefaultMutableTreeNode nodeToSelectAfter = null;

            // Chỉ thêm các lớp con cho Admin/Teacher
            if (currentUserRole == Role.ADMIN || currentUserRole == Role.TEACHER) {
                List<EduClass> classes = controller.getClassesForCurrentUser();
                if (classes != null) {
                    classes.sort(Comparator.comparing(EduClass::getClassName, String.CASE_INSENSITIVE_ORDER));
                    for (EduClass eduClass : classes) {
                        DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(eduClass);
                        resultsNode.add(classNode); // Thêm lớp vào dưới "Results & reviews"
                        if (selectedUserObjectBefore instanceof EduClass && eduClass.getClassId() == ((EduClass) selectedUserObjectBefore).getClassId()) {
                            nodeToSelectAfter = classNode;
                        }
                    }
                }
                // Cập nhật combobox bài tập (chỉ cần cho Admin/Teacher)
                if (controller != null) {
                    populateAssignmentClassComboBox(classes);
                }
            } else {
                populateAssignmentClassComboBox(Collections.emptyList()); // Xóa combobox
            }

            treeModel.reload(mainRootNode);
            classTree.expandPath(new TreePath(resultsNode.getPath()));
            classTree.expandPath(new TreePath(assignmentsNode.getPath()));

            if (nodeToSelectAfter != null) {
                if (selectedUserObjectBefore instanceof String && "Assignments".equals(selectedUserObjectBefore)) {
                    nodeToSelectAfter = assignmentsNode;
                } else {
                    nodeToSelectAfter = resultsNode;
                }
                TreePath pathToSelect = new TreePath(nodeToSelectAfter.getPath());
                classTree.setSelectionPath(pathToSelect);
                classTree.scrollPathToVisible(pathToSelect);
                handleTreeNodeSelection(nodeToSelectAfter);
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
        this.currentlyDisplayedAssignmentsInPanel = new ArrayList<>(assignments);
        assignmentTableModel.setRowCount(0);
        if (assignments != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Assignment assignment : assignments) {
                Vector<Object> row = new Vector<>();
                row.add(assignment.getAssignmentId());
                row.add(assignment.getTitle());
                row.add(assignment.getDueDateTime() != null ? assignment.getDueDateTime().format(formatter) : "Không có");
                row.add(assignment.getDescription());
                String statusText;
                if (assignment.getDueDateTime() == null) {
                    statusText = "Còn hạn làm bài";
                } else if (assignment.isOverdue()) {
                    statusText = "Hết hạn làm bài";
                } else {
                    statusText = "Còn hạn làm bài";
                }
                row.add(statusText);
                assignmentTableModel.addRow(row);
            }
        }
        updateAssignmentButtonStates();
        if (assignmentStatusTimer != null && !assignmentStatusTimer.isRunning() && assignments != null && !assignments.isEmpty()) {
            assignmentStatusTimer.start();
        } else if (assignmentStatusTimer != null && assignmentStatusTimer.isRunning() && (assignments == null || assignments.isEmpty())) {
            assignmentStatusTimer.stop(); // Dừng nếu không có bài tập
        }
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

    public int getColumnIndex(String columnName) {
        if (gradeTable == null || columnName == null) return -1;
        for (int i = 0; i < gradeTable.getColumnCount(); i++) {
            if (columnName.equals(gradeTable.getColumnName(i))) {
                return i;
            }
        }
        return -1;
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

                    if ("Info & Results".equals(nodeText) || "Results & reviews".equals(nodeText)) { // Kiểm tra cả hai tên
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
        if (obj1 instanceof Double && obj2 instanceof Double) {
            return Math.abs(((Double)obj1) - ((Double)obj2)) < 0.001;
        }
        return obj1.equals(obj2);
    }
    public void updateStudentInfoDisplay(Student student, EduClass studentClass) {
        if (student != null) {
            studentNameLabel.setText(student.getFullName());
            studentIdLabel.setText(String.valueOf(student.getStudentId()));
            studentDobLabel.setText(student.getDateOfBirth() != null ? student.getDateOfBirth().toString() : "N/A");
            studentGenderLabel.setText(student.getGender());
            studentPhoneLabel.setText(student.getPhone() != null ? student.getPhone() : "N/A");
            studentEmailLabel.setText(student.getEmail() != null ? student.getEmail() : "N/A");
            studentParentLabel.setText(student.getParentName() != null ? student.getParentName() : "N/A");

            if (studentClass != null) {
                studentClassLabel.setText(studentClass.getClassName() + " (Năm học: " + studentClass.getAcademicYear() + ", HK: " + studentClass.getSemester() + ")");
            } else {
                studentClassLabel.setText("N/A");
            }
            studentInfoPanel.setVisible(true);
        } else {
            studentNameLabel.setText("");
            studentIdLabel.setText("");
            studentDobLabel.setText("");
            studentGenderLabel.setText("");
            studentClassLabel.setText("");
            studentPhoneLabel.setText("");
            studentEmailLabel.setText("");
            studentParentLabel.setText("");
            studentInfoPanel.setVisible(false);
        }
    }
    private void checkAndUpdateAssignmentStatuses() {
        if (currentlyDisplayedAssignmentsInPanel == null || assignmentTableModel.getRowCount() == 0) {
            if(assignmentStatusTimer != null && assignmentStatusTimer.isRunning()){
                assignmentStatusTimer.stop();
            }
            return;
        }
        boolean needsButtonUpdate = false;

        for (int i = 0; i < assignmentTableModel.getRowCount(); i++) {
            try {
                int assignmentId = (int) assignmentTableModel.getValueAt(i, 0);
                Assignment assignmentFromCache = findAssignmentInCache(assignmentId);

                if (assignmentFromCache != null) {
                    boolean isNowOverdue = assignmentFromCache.isOverdue();
                    String currentStatusInTable = (String) assignmentTableModel.getValueAt(i, STATUS_COLUMN_INDEX);
                    String newStatusText;

                    if (assignmentFromCache.getDueDateTime() == null) {
                        newStatusText = "Còn hạn làm bài";
                    } else if (isNowOverdue) {
                        newStatusText = "Hết hạn làm bài";
                    } else {
                        newStatusText = "Còn hạn làm bài";
                    }

                    if (!Objects.equals(currentStatusInTable, newStatusText)) {
                        assignmentTableModel.setValueAt(newStatusText, i, STATUS_COLUMN_INDEX);
                        int selectedViewRow = assignmentTable.getSelectedRow();
                        if (selectedViewRow == i) {
                            needsButtonUpdate = true;
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("Error in assignment status timer: " + ex.getMessage());
                 assignmentStatusTimer.stop();
            }
        }

        if (needsButtonUpdate) {
            updateAssignmentButtonStates();
        }
    }

    private Assignment findAssignmentInCache(int assignmentId) {
        if (currentlyDisplayedAssignmentsInPanel != null) {
            for (Assignment assignment : currentlyDisplayedAssignmentsInPanel) {
                if (assignment.getAssignmentId() == assignmentId) {
                    return assignment;
                }
            }
        }
        return null;
    }
    private int getColumnIndexFromView(String columnName) {
        if (gradeTable == null || gradeTable.getColumnModel() == null || columnName == null) {
            System.err.println("getColumnIndexFromView: Pre-check failed for column: " + columnName);
            return -1;
        }
        TableColumnModel cm = gradeTable.getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            TableColumn column = cm.getColumn(i);
            if (column != null && columnName.equals(column.getHeaderValue())) { // So sánh HeaderValue
                return i; // Trả về view index
            }
        }
        // System.out.println("getColumnIndexFromView: Column '" + columnName + "' not found in current view model.");
        return -1; // Không tìm thấy cột trong TableColumnModel hiện tại
    }

}