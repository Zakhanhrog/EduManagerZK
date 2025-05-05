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
import javax.swing.table.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Vector;
import javax.swing.event.TableModelEvent;

public class EducationPanel extends JPanel {

    private EducationController controller;
    private MainController mainController;
    private Role currentUserRole;

    private JSplitPane splitPane;
    private JPanel leftPanel;
    private JPanel rightPanel;

    private JList<EduClass> classList;
    private DefaultListModel<EduClass> classListModel;
    private JScrollPane classListScrollPane;
    private JLabel assignmentsPlaceholder;

    private JTable gradeTable;
    private DefaultTableModel gradeTableModel;
    private JScrollPane gradeTableScrollPane;
    private JButton exportButton;
    private JLabel selectedClassLabel;

    private boolean isEditing = false;
    private JButton editButton;
    private JButton cancelEditButton;
    private JButton saveChangesButton; // <<< ĐỔI TÊN TỪ saveButton

    private static final DecimalFormat df = new DecimalFormat("#.00"); // <<< ĐỔI FORMAT
    private static final String[] SUBJECT_KEYS = {"Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD", "Nghệ thuật"};
    private static final String[] EDITABLE_SUBJECT_KEYS = {"Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD"};
    private static final String ART_KEY = "Nghệ thuật";
    private static final String CONDUCT_KEY = "Hạnh kiểm";
    private static final String[] TABLE_COLUMNS = {"STT", "Tên HS", "Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD", ART_KEY, "TB KHTN", "TB KHXH", "TB môn học", CONDUCT_KEY};

    private boolean hasPendingChanges = false;

    public EducationPanel() {
        setLayout(new BorderLayout());
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
            configureControlsForRole(this.currentUserRole); // <<< SỬA GỌI HÀM NÀY
        } else {
            // Xử lý trường hợp controller null nếu cần (ví dụ: ẩn panel)
            this.removeAll();
            this.add(new JLabel("Education module not available.", SwingConstants.CENTER));
            this.revalidate();
            this.repaint();
        }
    }

    private void initComponents() {
        // --- Khởi tạo các Buttons trước khi set trạng thái ---
        editButton = new JButton("Chỉnh Sửa");
        editButton.setIcon(UIUtils.loadSVGIcon("/icons/edit.svg", 16)); // Thêm icon
        cancelEditButton = new JButton("Hủy Bỏ");
        saveChangesButton = new JButton("Lưu Thay Đổi");
        saveChangesButton.setIcon(UIUtils.loadSVGIcon("/icons/save.svg", 16));
        exportButton = new JButton("Xuất Excel");
        exportButton.setIcon(UIUtils.loadSVGIcon("/icons/export.svg", 16));

        // --- Thiết lập trạng thái ban đầu cho các nút ---
        editButton.setEnabled(false);
        saveChangesButton.setVisible(false);
        cancelEditButton.setVisible(false);
        exportButton.setEnabled(false);

        // --- Left Panel ---
        leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        classListModel = new DefaultListModel<>();
        classList = new JList<>(classListModel);
        classList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        classList.setCellRenderer(new EduClassListCellRenderer());
        classListScrollPane = new JScrollPane(classList);
        classListScrollPane.setBorder(new TitledBorder("Chọn Lớp"));

        assignmentsPlaceholder = new JLabel("<html><i>(Phần Giao bài tập<br>sẽ phát triển sau)</i></html>", SwingConstants.CENTER);
        assignmentsPlaceholder.setBorder(new TitledBorder("Giao Bài Tập"));
        assignmentsPlaceholder.setPreferredSize(new Dimension(150, 100));

        // --- Right Panel ---
        rightPanel = new JPanel(new BorderLayout(5, 10));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        selectedClassLabel = new JLabel("Chưa chọn lớp", SwingConstants.CENTER);
        selectedClassLabel.setFont(selectedClassLabel.getFont().deriveFont(Font.BOLD));

        // Grade Table Model
        gradeTableModel = new DefaultTableModel(TABLE_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (isEditing && controller != null && controller.canCurrentUserEdit()) { // <<< KIỂM TRA isEditing
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
                if (columnIndex <= 1) return String.class; // STT, Tên HS
                return Double.class; // Các cột điểm và TB
            }
        };

        gradeTable = new JTable(gradeTableModel);
        gradeTable.setRowHeight(25);
        gradeTable.getTableHeader().setReorderingAllowed(false);
        gradeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setupTableEditorsAndRenderers(); // Gọi sau khi table được tạo
        gradeTableScrollPane = new JScrollPane(gradeTable);
//        gradeTableScrollPane.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
        gradeTableScrollPane.setBorder(BorderFactory.createTitledBorder("Bảng điểm chi tiết"));

        // --- Split Pane ---
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.15);
        splitPane.setDividerSize(8);
    }

    private void setupTableEditorsAndRenderers() {
        // --- Renderer cho các cột điểm và TB ---
        DefaultTableCellRenderer numberRenderer = new DefaultTableCellRenderer() {
            private final DecimalFormat avgFormat = new DecimalFormat("#.00"); // Format 2 chữ số

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.RIGHT); // Căn phải
                if (value instanceof Number) {
                    label.setText(avgFormat.format(((Number) value).doubleValue())); // Format #.00
                } else {
                    label.setText(""); // Để trống nếu null
                }
                return label;
            }
        };

        // Áp dụng cho các cột điểm số và cột TB
        for (int i = 2; i < TABLE_COLUMNS.length; i++) { // Bắt đầu từ cột "Toán"
            String colName = TABLE_COLUMNS[i];
            // Chỉ áp dụng format số cho cột điểm và cột TB
            if (!colName.equals(ART_KEY) && !colName.equals(CONDUCT_KEY)) {
                gradeTable.getColumnModel().getColumn(i).setCellRenderer(numberRenderer);
            }
        }

        // --- Editor và Renderer cho cột Enum (Nghệ thuật, Hạnh kiểm) ---
        setupEnumColumn(ART_KEY, ArtStatus.values());
        setupEnumColumn(CONDUCT_KEY, ConductRating.values());

        // --- Editor cho các cột điểm số (JTextField) ---
        JTextField numberTextField = new JTextField();
        numberTextField.setHorizontalAlignment(JTextField.RIGHT);
        // TODO: Thêm DocumentFilter để giới hạn nhập liệu nếu cần
        DefaultCellEditor numberEditor = new DefaultCellEditor(numberTextField);
        for (String key : EDITABLE_SUBJECT_KEYS) {
            int colIndex = getColumnIndex(key);
            if (colIndex != -1) {
                gradeTable.getColumnModel().getColumn(colIndex).setCellEditor(numberEditor);
            }
        }

        // --- Điều chỉnh độ rộng cột ---
        setColumnWidths();
    }

    // --- Helper setup cột Enum ---
    private <E extends Enum<E>> void setupEnumColumn(String columnName, E[] enumValues) {
        int columnIndex = getColumnIndex(columnName);
        if (columnIndex != -1) {
            TableColumn column = gradeTable.getColumnModel().getColumn(columnIndex);
            JComboBox<E> comboBox = new JComboBox<>(enumValues);
            column.setCellEditor(new DefaultCellEditor(comboBox));
            // Renderer mặc định cho ComboBox thường hiển thị toString() của Enum, đã ổn
            // Nếu muốn tùy chỉnh thêm cách hiển thị trong ô không sửa, tạo Renderer riêng
        }
    }

    // --- Helper đặt độ rộng cột ---
    private void setColumnWidths(){
        try { // Thêm try-catch vì getColumnIndex có thể trả về -1
            gradeTable.getColumnModel().getColumn(getColumnIndex("STT")).setMaxWidth(40);
            gradeTable.getColumnModel().getColumn(getColumnIndex("Tên HS")).setPreferredWidth(180);
            gradeTable.getColumnModel().getColumn(getColumnIndex(ART_KEY)).setMinWidth(100);
            gradeTable.getColumnModel().getColumn(getColumnIndex(CONDUCT_KEY)).setMinWidth(100);
            gradeTable.getColumnModel().getColumn(getColumnIndex("TB KHTN")).setPreferredWidth(80);
            gradeTable.getColumnModel().getColumn(getColumnIndex("TB KHXH")).setPreferredWidth(80);
            gradeTable.getColumnModel().getColumn(getColumnIndex("TB môn học")).setPreferredWidth(90);
            // Đặt độ rộng mặc định cho các cột điểm
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
        // Left Panel Layout
        JPanel assignmentWrapper = new JPanel(new BorderLayout());
        assignmentWrapper.add(assignmentsPlaceholder, BorderLayout.NORTH);
        leftPanel.add(classListScrollPane, BorderLayout.CENTER);
        leftPanel.add(assignmentWrapper, BorderLayout.SOUTH);

        // Right Panel Layout
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); // Thêm khoảng cách ngang
        buttonPanel.add(editButton);
        buttonPanel.add(cancelEditButton);
        buttonPanel.add(saveChangesButton); // <<< DÙNG TÊN MỚI
        buttonPanel.add(exportButton); // Chuyển nút Export lên đây cho gọn

        JPanel topOfRightPanel = new JPanel(new BorderLayout(10,0)); // Khoảng cách giữa label và nút
        topOfRightPanel.add(selectedClassLabel, BorderLayout.CENTER);
        topOfRightPanel.add(buttonPanel, BorderLayout.EAST);
        topOfRightPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0)); // Padding dưới

        rightPanel.add(topOfRightPanel, BorderLayout.NORTH);
        rightPanel.add(gradeTableScrollPane, BorderLayout.CENTER);

        // Split Pane Setup
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        // Add Split Pane to the main panel
        add(splitPane, BorderLayout.CENTER);
    }

    private void setupActions() {
        // --- Chọn lớp ---
        classList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && controller != null) {
                if (isEditing && hasPendingChanges) {
                    if (!UIUtils.showConfirmDialog(this, "Unsaved Changes", "Discard unsaved changes and load new class?")) {
                        return;
                    }
                }
                setEditingMode(false); // <<< TẮT EDIT KHI CHỌN LỚP KHÁC

                EduClass selectedClass = classList.getSelectedValue();
                if (selectedClass != null) {
                    selectedClassLabel.setText("Class: " + selectedClass.getClassName());
                    controller.loadDataForClass(selectedClass.getClassId());
                    exportButton.setEnabled(true);
                } else {
                    selectedClassLabel.setText("No class selected");
                    gradeTableModel.setRowCount(0);
                    editButton.setEnabled(false); // <<< VÔ HIỆU HÓA EDIT KHI KHÔNG CÓ LỚP
                    exportButton.setEnabled(false);
                    markChangesPending(false); // Reset flag
                }
            }
        });

        // --- Thay đổi dữ liệu bảng ---
        gradeTableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && isEditing) { // <<< CHỈ XỬ LÝ KHI ĐANG EDIT
                int row = e.getFirstRow();
                int column = e.getColumn();
                if (controller != null && row >= 0 && column >= 0) {
                    String columnName = gradeTableModel.getColumnName(column);
                    boolean isDataColumn = false; // Kiểm tra xem có phải cột điểm/hk/nt không
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

        // --- Nút Edit ---
        editButton.addActionListener(e -> {
            setEditingMode(true);
            UIUtils.showInfoMessage(this, "Edit Mode", "You can now edit grades and conduct.");
        });

        // --- Nút Cancel Edit ---
        cancelEditButton.addActionListener(e -> {
            if (hasPendingChanges) {
                if (!UIUtils.showConfirmDialog(this, "Discard Changes?", "Are you sure you want to discard all unsaved changes?")) {
                    return;
                }
            }
            setEditingMode(false);
            // Tải lại dữ liệu gốc cho lớp hiện tại
            if(controller != null && controller.getCurrentSelectedClassId() > 0){
                System.out.println("Cancelling edit, reloading data for class: " + controller.getCurrentSelectedClassId());
                controller.loadDataForClass(controller.getCurrentSelectedClassId());
            }
        });

        // --- Nút Save Changes ---
        saveChangesButton.addActionListener(e -> { // <<< DÙNG TÊN MỚI
            if (controller != null) {
                controller.saveAllChanges();
                // Controller sẽ gọi markChangesPending(false) nếu lưu thành công
                if (!hasPendingChanges) { // Kiểm tra lại flag
                    setEditingMode(false); // Tự động tắt chế độ sửa nếu lưu thành công
                }
            }
        });

        // --- Nút Export ---
        exportButton.addActionListener(e -> {
            if (controller != null && mainController != null && controller.getCurrentSelectedClassId() > 0) {
                mainController.requestExcelExport(MainController.EXPORT_GRADES, controller.getCurrentSelectedClassId());
            } else {
                if(mainController == null) System.err.println("Export Error: MainController reference is null in EducationPanel.");
                UIUtils.showWarningMessage(this, "Cannot Export", "Please select a class first.");
            }
        });
    }

    // --- Cấu hình hiển thị dựa trên Role ---
    public void configureControlsForRole(Role userRole) {
        this.currentUserRole = userRole;
        System.out.println("EducationPanel: Configuring controls for role: " + userRole);

        boolean canEdit = (controller != null && controller.canCurrentUserEdit());

        if (splitPane == null) { // Kiểm tra nếu components chưa được tạo
            System.err.println("EducationPanel: Components not initialized in configureControlsForRole.");
            return; // Không thể cấu hình nếu chưa init
        }


        switch (userRole) {
            case ADMIN:
            case TEACHER:
                splitPane.setLeftComponent(leftPanel);
                splitPane.setDividerSize(8);
                splitPane.setVisible(true);
                if (leftPanel != null) leftPanel.setVisible(true);
                if (rightPanel != null) {
                    Component topComponent = ((BorderLayout)rightPanel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
                    if (topComponent != null) topComponent.setVisible(true);
                    // Hiển thị các nút nhưng trạng thái enable/disable tùy vào quyền và dữ liệu
                    if (editButton != null) editButton.setVisible(true);
                    if (cancelEditButton != null) cancelEditButton.setVisible(isEditing); // Chỉ hiện khi đang sửa
                    if (saveChangesButton != null) saveChangesButton.setVisible(isEditing); // Chỉ hiện khi đang sửa
                    if (exportButton != null) exportButton.setVisible(true);
                }
                reloadClassList();
                setEditingMode(false); // Luôn bắt đầu ở chế độ xem
                // Cập nhật trạng thái enable/disable nút dựa trên lớp được chọn (nếu có) và quyền
                EduClass selectedClass = (classList != null) ? classList.getSelectedValue() : null;
                boolean classSelected = (selectedClass != null);
                if(editButton!= null) editButton.setEnabled(classSelected && canEdit);
                if(exportButton != null) exportButton.setEnabled(classSelected && gradeTableModel.getRowCount() > 0);
                if(saveChangesButton != null) saveChangesButton.setEnabled(isEditing && hasPendingChanges && canEdit);
                if(cancelEditButton != null) cancelEditButton.setVisible(isEditing);

                break;

            case STUDENT:
                splitPane.setLeftComponent(null);
                splitPane.setDividerSize(0);
                splitPane.setVisible(true);
                if (leftPanel != null) leftPanel.setVisible(false);
                if (rightPanel != null) {
                    Component topComponent = ((BorderLayout)rightPanel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
                    if (topComponent != null) topComponent.setVisible(false); // Ẩn phần nút và label lớp
                }
                if(controller != null) controller.loadDataForCurrentStudent(); // Tải điểm cho student
                setEditingMode(false); // Student không bao giờ sửa
                // Ẩn tất cả các nút action
                if(editButton != null) editButton.setVisible(false);
                if(cancelEditButton != null) cancelEditButton.setVisible(false);
                if(saveChangesButton != null) saveChangesButton.setVisible(false);
                if(exportButton != null) exportButton.setVisible(false);
                if (rightPanel != null) rightPanel.setBorder(BorderFactory.createTitledBorder("Bảng điểm cá nhân"));
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

    // --- Tải lại danh sách lớp (Giữ nguyên) ---
    public void reloadClassList() {
        if (controller != null && classListModel != null) {
            System.out.println("EducationPanel: Reloading class list.");
            EduClass selectedBefore = classList.getSelectedValue();
            classListModel.clear();
            List<EduClass> classes = controller.getClassesForCurrentUser();
            if (classes != null) {
                classes.forEach(classListModel::addElement);
            }
            if (selectedBefore != null) {
                for(int i=0; i<classListModel.getSize(); i++){
                    if(classListModel.getElementAt(i).getClassId() == selectedBefore.getClassId()){
                        classList.setSelectedIndex(i);
                        break;
                    }
                }
                if(classList.getSelectedValue() == null || classList.getSelectedValue().getClassId() != selectedBefore.getClassId()){
                    classList.clearSelection();
                    selectedClassLabel.setText("Chưa chọn lớp");
                    gradeTableModel.setRowCount(0);
                    setEditingMode(false); // Tắt edit khi không có lớp được chọn
                    if(editButton != null) editButton.setEnabled(false);
                    if(exportButton != null) exportButton.setEnabled(false);
                }
            } else {
                // Nếu không có lớp nào được chọn sau khi reload
                classList.clearSelection();
                selectedClassLabel.setText("Chưa chọn lớp");
                gradeTableModel.setRowCount(0);
                setEditingMode(false);
                if(editButton != null) editButton.setEnabled(false);
                if(exportButton != null) exportButton.setEnabled(false);
            }
            System.out.println("EducationPanel: Class list reloaded with " + classListModel.getSize() + " items.");
        }
    }

    // --- Cập nhật bảng điểm (Admin/Teacher) (Giữ nguyên) ---
    public void updateTableData(List<Student> students, List<AcademicRecord> records) {
        gradeTableModel.setRowCount(0);
        if (students == null || records == null || students.size() != records.size()) {
            System.err.println("Error updating table: Mismatch/null student/record lists.");
            setEditingMode(false); // Tắt edit nếu lỗi data
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
        markChangesPending(false); // Reset flag khi tải data mới
        setEditingMode(false);     // Luôn tắt edit khi tải data mới
        // Cập nhật trạng thái nút dựa trên quyền và dữ liệu mới
        boolean canEdit = (controller != null && controller.canCurrentUserEdit());
        if(editButton != null) editButton.setEnabled(canEdit && gradeTableModel.getRowCount() > 0);
        if(exportButton != null) exportButton.setEnabled(gradeTableModel.getRowCount() > 0);
    }

    // --- Cập nhật bảng điểm cho Student (Giữ nguyên) ---
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
        // Đảm bảo các nút bị ẩn/vô hiệu hóa cho student
        setEditingMode(false);
        if(editButton != null) editButton.setVisible(false);
        if(cancelEditButton != null) cancelEditButton.setVisible(false);
        if(saveChangesButton != null) saveChangesButton.setVisible(false);
        if(exportButton != null) exportButton.setVisible(false);
    }

    // --- Cập nhật các ô tính toán (Giữ nguyên) ---
    public void updateCalculatedValues(int rowIndex, AcademicRecord record) {
        if (rowIndex >= 0 && rowIndex < gradeTableModel.getRowCount()) {
            try { // Thêm try-catch để tránh lỗi nếu getColumnIndex trả về -1
                gradeTableModel.setValueAt(record.calculateAvgNaturalSciences(), rowIndex, getColumnIndex("TB KHTN"));
                gradeTableModel.setValueAt(record.calculateAvgSocialSciences(), rowIndex, getColumnIndex("TB KHXH"));
                gradeTableModel.setValueAt(record.calculateAvgOverallSubjects(), rowIndex, getColumnIndex("TB môn học"));
            } catch(ArrayIndexOutOfBoundsException e) {
                System.err.println("Error updating calculated values, column index likely invalid: " + e.getMessage());
            }
        }
    }

    // --- Đánh dấu có thay đổi chưa lưu (SỬA ĐỂ DÙNG saveChangesButton) ---
    public void markChangesPending(boolean pending) {
        this.hasPendingChanges = pending;
        if (saveChangesButton != null) {
            // Chỉ bật nút Save khi đang ở chế độ sửa, có thay đổi, và có quyền
            saveChangesButton.setEnabled(isEditing && pending && controller != null && controller.canCurrentUserEdit());
        }
    }

    // --- Vẽ lại ô (Giữ nguyên) ---
    public void refreshTableCell(int rowIndex, String subjectKey){
        if(rowIndex >= 0 && rowIndex < gradeTableModel.getRowCount()){
            int columnIndex = getColumnIndex(subjectKey);
            if(columnIndex != -1){
                System.out.println("Requesting repaint for cell: row=" + rowIndex + ", col=" + columnIndex);
                gradeTableModel.fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }

    // --- Helper lấy index cột (Thêm kiểm tra null cho gradeTable) ---
    public int getColumnIndex(String columnName) {
        if (gradeTable == null) return -1; // Trả về -1 nếu bảng chưa được tạo
        for (int i = 0; i < gradeTable.getColumnCount(); i++) {
            if (gradeTable.getColumnName(i).equals(columnName)) {
                return i;
            }
        }
        System.err.println("Warning: Column not found in gradeTable: " + columnName); // Log nếu không tìm thấy
        return -1;
    }

    // --- Helper lấy TableModel (THÊM MỚI) ---
    public DefaultTableModel getGradeTableModel() {
        return gradeTableModel;
    }


    // --- Renderer cho JList (Giữ nguyên) ---
    private static class EduClassListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof EduClass) {
                EduClass eduClass = (EduClass) value;
                label.setText(eduClass.getClassName());
            } else {
                label.setText(value == null ? "" : value.toString()); // Xử lý các trường hợp khác
            }
            return label;
        }
    }

    // --- Phương thức quản lý chế độ Edit (THÊM MỚI/SỬA LẠI) ---
    private void setEditingMode(boolean editing) {
        this.isEditing = editing;

        // Cập nhật trạng thái hiển thị và enable của các nút
        // Thêm kiểm tra null để đảm bảo các nút đã được khởi tạo
        if (editButton != null) {
            editButton.setVisible(!editing);
            // Chỉ bật nút Edit nếu không đang sửa, có controller, có quyền, và có dữ liệu trong bảng
            editButton.setEnabled(!editing && controller != null && controller.canCurrentUserEdit() && (gradeTableModel != null && gradeTableModel.getRowCount() > 0));
        }
        if (cancelEditButton != null) {
            cancelEditButton.setVisible(editing); // Hiện khi đang sửa
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