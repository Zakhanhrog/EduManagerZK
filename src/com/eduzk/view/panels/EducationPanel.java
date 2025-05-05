package com.eduzk.view.panels;

import com.eduzk.controller.EducationController;
import com.eduzk.model.entities.AcademicRecord;
import com.eduzk.model.entities.EduClass;
import com.eduzk.model.entities.Role;
import com.eduzk.model.entities.Student;
import com.eduzk.model.entities.ArtStatus;
import com.eduzk.model.entities.ConductRating;
import com.eduzk.utils.UIUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class EducationPanel extends JPanel {

    private EducationController controller;
    private Role currentUserRole; // Lưu role để biết cách hiển thị

    // --- Components ---
    private JSplitPane splitPane;
    private JPanel leftPanel;
    private JPanel rightPanel;

    // Left Panel Components
    private JList<EduClass> classList;
    private DefaultListModel<EduClass> classListModel;
    private JScrollPane classListScrollPane;
    // Placeholder for Assignments
    private JLabel assignmentsPlaceholder;

    // Right Panel Components
    private JTable gradeTable;
    private DefaultTableModel gradeTableModel;
    private JScrollPane gradeTableScrollPane;
    private JButton saveButton;
    private JButton exportButton; // Thêm nút Export
    private JLabel selectedClassLabel; // Hiển thị tên lớp đang chọn

    // Định dạng số thập phân cho điểm trung bình
    private static final DecimalFormat df = new DecimalFormat("#.##");
    // Key môn học (PHẢI THỐNG NHẤT VỚI CONTROLLER VÀ ENTITY)
    private static final String[] SUBJECT_KEYS = {"Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD", "Nghệ thuật"};
    private static final String[] EDITABLE_SUBJECT_KEYS = {"Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD"}; // Các môn nhập điểm số
    private static final String ART_KEY = "Nghệ thuật";
    private static final String CONDUCT_KEY = "Hạnh kiểm";
    // Thứ tự cột trong bảng
    private static final String[] TABLE_COLUMNS = {"STT", "Tên HS", "Toán", "Văn", "Anh", "Lí", "Hoá", "Sinh", "Sử", "Địa", "GDCD", ART_KEY, "TB KHTN", "TB KHXH", "TB môn học", CONDUCT_KEY};

    private boolean hasPendingChanges = false;

    public EducationPanel() {
        setLayout(new BorderLayout());
        // Controller sẽ được set sau qua setController
    }

    public void setController(EducationController controller, Role userRole) {
        this.controller = controller;
        this.currentUserRole = userRole;
        if (this.controller != null) {
            this.controller.setEducationPanel(this);
            initComponents(); // Khởi tạo component sau khi có controller và role
            setupLayout();
            setupActions();
            configureViewForRole(); // Cấu hình hiển thị ban đầu
        }
    }

    private void initComponents() {
        // --- Left Panel ---
        leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        classListModel = new DefaultListModel<>();
        classList = new JList<>(classListModel);
        classList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        classList.setCellRenderer(new EduClassListCellRenderer()); // Renderer để hiển thị tên lớp đẹp hơn
        classListScrollPane = new JScrollPane(classList);
        classListScrollPane.setBorder(new TitledBorder("Chọn Lớp"));

        assignmentsPlaceholder = new JLabel("<html><i>(Phần Giao bài tập<br>sẽ phát triển sau)</i></html>", SwingConstants.CENTER);
        assignmentsPlaceholder.setBorder(new TitledBorder("Giao Bài Tập"));
        assignmentsPlaceholder.setPreferredSize(new Dimension(150, 100)); // Kích thước tạm thời

        // --- Right Panel ---
        rightPanel = new JPanel(new BorderLayout(5, 10));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        selectedClassLabel = new JLabel("Chưa chọn lớp", SwingConstants.CENTER);
        selectedClassLabel.setFont(selectedClassLabel.getFont().deriveFont(Font.BOLD));

        // Grade Table Model
        gradeTableModel = new DefaultTableModel(TABLE_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Chỉ cho phép sửa nếu là Admin/Teacher và là cột điểm/hạnh kiểm/nghệ thuật
                if (controller != null && controller.canCurrentUserEdit()) {
                    String colName = getColumnName(column);
                    // Cho phép sửa các cột điểm số, Nghệ thuật và Hạnh kiểm
                    for(String key : EDITABLE_SUBJECT_KEYS) {
                        if (key.equals(colName)) return true;
                    }
                    return ART_KEY.equals(colName) || CONDUCT_KEY.equals(colName);
                }
                return false;
            }

            // Định nghĩa kiểu dữ liệu cho cột để render và editor hoạt động đúng
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                String colName = getColumnName(columnIndex);
                if (ART_KEY.equals(colName)) return ArtStatus.class;
                if (CONDUCT_KEY.equals(colName)) return ConductRating.class;
                // Cột STT và Tên HS là không phải số
                if (columnIndex <= 1) return String.class;
                // Các cột điểm và TB là số (Double)
                return Double.class; // Hoặc dùng Number.class
            }
        };

        gradeTable = new JTable(gradeTableModel);
        gradeTable.setRowHeight(25); // Tăng chiều cao hàng cho dễ nhìn
        gradeTable.getTableHeader().setReorderingAllowed(false); // Không cho kéo thả cột
        gradeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Chọn 1 hàng thôi
        // Set editor và renderer cho cột đặc biệt
        setupTableEditorsAndRenderers();
        gradeTableScrollPane = new JScrollPane(gradeTable);

        // Buttons
        saveButton = new JButton("Lưu Thay Đổi");
        saveButton.setIcon(UIUtils.loadSVGIcon("/icons/save.svg", 16)); // Giả sử có hàm load icon
        saveButton.setEnabled(false); // Ban đầu vô hiệu hóa

        exportButton = new JButton("Xuất Excel");
        exportButton.setIcon(UIUtils.loadSVGIcon("/icons/export.svg", 16));
        exportButton.setEnabled(false); // Chỉ enable khi có dữ liệu

        // --- Split Pane ---
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.15); // Sidebar chiếm 15% chiều rộng ban đầu
        splitPane.setDividerSize(8);
    }

    private void setupTableEditorsAndRenderers() {
        // Renderer cho các ô điểm (căn phải) và format số
        DefaultTableCellRenderer numberRenderer = new DefaultTableCellRenderer();
        numberRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        DecimalFormat gradeFormat = new DecimalFormat("#.#"); // Format 1 chữ số thập phân
        // Áp dụng cho các cột điểm số
        for (int i = 2; i < TABLE_COLUMNS.length; i++) { // Bắt đầu từ cột "Toán"
            String colName = TABLE_COLUMNS[i];
            boolean isCalculated = colName.startsWith("TB"); // Cột TB không cần renderer số? Hoặc cần format khác
            boolean isGradeColumn = List.of(EDITABLE_SUBJECT_KEYS).contains(colName);
            if(isGradeColumn || isCalculated){ // Áp dụng cho cột điểm và cột TB
                TableColumn column = gradeTable.getColumnModel().getColumn(i);
                column.setCellRenderer(new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        label.setHorizontalAlignment(SwingConstants.RIGHT);
                        if (value instanceof Number) {
                            // Format điểm TB hoặc điểm thường
                            label.setText(df.format(((Number) value).doubleValue()));
                        } else if (value == null && isGradeColumn) {
                            label.setText(""); // Hiển thị trống nếu điểm null
                        }
                        return label;
                    }
                });
            }
        }


        // Editor và Renderer cho cột Nghệ thuật (ComboBox)
        TableColumn artColumn = gradeTable.getColumnModel().getColumn(getColumnIndex(ART_KEY));
        JComboBox<ArtStatus> artComboBox = new JComboBox<>(ArtStatus.values());
        artColumn.setCellEditor(new DefaultCellEditor(artComboBox));
        artColumn.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                // Hiển thị toString() của Enum (Đạt/Không đạt)
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        // Editor và Renderer cho cột Hạnh kiểm (ComboBox)
        TableColumn conductColumn = gradeTable.getColumnModel().getColumn(getColumnIndex(CONDUCT_KEY));
        JComboBox<ConductRating> conductComboBox = new JComboBox<>(ConductRating.values());
        conductColumn.setCellEditor(new DefaultCellEditor(conductComboBox));
        conductColumn.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                // Hiển thị toString() của Enum
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        // --- Editor cho các cột điểm số (JTextField với validation) ---
        // Sử dụng DefaultCellEditor nhưng có thể tùy chỉnh thêm nếu cần validation phức tạp hơn
        JTextField numberTextField = new JTextField();
        numberTextField.setHorizontalAlignment(JTextField.RIGHT);
        // Có thể thêm DocumentFilter để chỉ cho nhập số và trong khoảng 0-10
        DefaultCellEditor numberEditor = new DefaultCellEditor(numberTextField);
        for (String key : EDITABLE_SUBJECT_KEYS) {
            TableColumn gradeColumn = gradeTable.getColumnModel().getColumn(getColumnIndex(key));
            gradeColumn.setCellEditor(numberEditor);
        }


        // --- Điều chỉnh độ rộng cột (sau khi đã có renderer/editor) ---
        gradeTable.getColumnModel().getColumn(getColumnIndex("STT")).setMaxWidth(40);
        gradeTable.getColumnModel().getColumn(getColumnIndex("Tên HS")).setPreferredWidth(180);
        // Đặt độ rộng cố định hoặc min/max cho các cột điểm nếu muốn
        gradeTable.getColumnModel().getColumn(getColumnIndex(ART_KEY)).setMinWidth(100);
        gradeTable.getColumnModel().getColumn(getColumnIndex(CONDUCT_KEY)).setMinWidth(100);
        gradeTable.getColumnModel().getColumn(getColumnIndex("TB KHTN")).setPreferredWidth(80);
        gradeTable.getColumnModel().getColumn(getColumnIndex("TB KHXH")).setPreferredWidth(80);
        gradeTable.getColumnModel().getColumn(getColumnIndex("TB môn học")).setPreferredWidth(90);
    }

    private void setupLayout() {
        // Left Panel Layout
        JPanel assignmentWrapper = new JPanel(new BorderLayout());
        assignmentWrapper.add(assignmentsPlaceholder, BorderLayout.NORTH); // Đặt placeholder vào wrapper
        leftPanel.add(classListScrollPane, BorderLayout.CENTER);
        leftPanel.add(assignmentWrapper, BorderLayout.SOUTH);

        // Right Panel Layout
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(exportButton);
        buttonPanel.add(saveButton);

        JPanel topOfRightPanel = new JPanel(new BorderLayout());
        topOfRightPanel.add(selectedClassLabel, BorderLayout.CENTER);
        topOfRightPanel.add(buttonPanel, BorderLayout.EAST); // Đặt nút ở bên phải tiêu đề lớp

        rightPanel.add(topOfRightPanel, BorderLayout.NORTH);
        rightPanel.add(gradeTableScrollPane, BorderLayout.CENTER);

        // Split Pane Setup
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        // Add Split Pane to the main panel
        add(splitPane, BorderLayout.CENTER);
    }

    private void setupActions() {
        // --- Xử lý chọn lớp trong JList ---
        classList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && controller != null) {
                // Kiểm tra xem có thay đổi chưa lưu không
                if (hasPendingChanges) {
                    if (!UIUtils.showConfirmDialog(this, "Unsaved Changes", "You have unsaved changes. Discard them and load the new class?")) {
                        // Người dùng không muốn bỏ thay đổi -> chọn lại mục cũ
                        // (Cần lưu lại index cũ để làm việc này) - TODO: Implement rollback selection
                        return; // Không làm gì cả
                    }
                }

                EduClass selectedClass = classList.getSelectedValue();
                if (selectedClass != null) {
                    selectedClassLabel.setText("Lớp: " + selectedClass.getClassName());
                    controller.loadDataForClass(selectedClass.getClassId());
                    saveButton.setEnabled(false); // Reset nút Save khi tải lớp mới
                    hasPendingChanges = false;
                    exportButton.setEnabled(true); // Có dữ liệu để export
                } else {
                    selectedClassLabel.setText("Chưa chọn lớp");
                    gradeTableModel.setRowCount(0); // Xóa bảng
                    saveButton.setEnabled(false);
                    hasPendingChanges = false;
                    exportButton.setEnabled(false);
                }
            }
        });

        // --- Xử lý sự kiện thay đổi dữ liệu trong TableModel ---
        gradeTableModel.addTableModelListener(e -> {
            // Chỉ quan tâm khi dữ liệu thực sự thay đổi (không phải header,...)
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                // Chỉ xử lý nếu controller đã sẵn sàng và là cột có thể sửa
                if (controller != null && row >= 0 && column >= 0 ) {
                    String columnName = gradeTableModel.getColumnName(column);
                    // Kiểm tra xem cột có phải là cột có thể gây ra thay đổi cần lưu không
                    boolean isEditableColumn = false;
                    for(String key : EDITABLE_SUBJECT_KEYS) { if (key.equals(columnName)) {isEditableColumn = true; break;} }
                    if(!isEditableColumn) isEditableColumn = ART_KEY.equals(columnName) || CONDUCT_KEY.equals(columnName);

                    if(isEditableColumn) {
                        Object newValue = gradeTableModel.getValueAt(row, column);
                        System.out.println("Table cell changed: row=" + row + ", col=" + column + ", name=" + columnName + ", value=" + newValue);
                        // Gọi controller để cập nhật dữ liệu trong bộ nhớ và tính toán lại
                        controller.updateRecordInMemory(row, columnName, newValue);
                        // hasPendingChanges sẽ được set bởi controller sau khi gọi updateCalculatedValues
                    }
                }
            }
        });

        // --- Xử lý nút Lưu ---
        saveButton.addActionListener(e -> {
            if (controller != null) {
                controller.saveAllChanges();
                // Trạng thái nút Save sẽ được cập nhật bởi controller sau khi lưu
            }
        });

        // --- Xử lý nút Export ---
        exportButton.addActionListener(e -> {
            if (controller != null && controller.getCurrentSelectedClassId() > 0) {
                // TODO: Gọi MainController để hiển thị dialog chọn file và thực hiện export
                // MainController sẽ gọi lại EducationController.getGradeDataForExport()
                System.out.println("Export requested for class ID: " + controller.getCurrentSelectedClassId());
                // Ví dụ: mainController.requestExcelExport(MainController.EXPORT_GRADES, controller.getCurrentSelectedClassId());
                JOptionPane.showMessageDialog(this, "Chức năng Export đang được phát triển!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            } else {
                UIUtils.showWarningMessage(this, "No Class Selected", "Please select a class to export grades.");
            }
        });
    }

    // --- Cấu hình hiển thị dựa trên vai trò ---
    private void configureViewForRole() {
        if (controller == null) return;

        switch (currentUserRole) {
            case ADMIN:
            case TEACHER:
                // Hiển thị danh sách lớp
                splitPane.setLeftComponent(leftPanel); // Đảm bảo sidebar hiển thị
                leftPanel.setVisible(true);
                loadClassList();
                // Các nút action sẽ được bật/tắt dựa trên quyền sửa (controller.canCurrentUserEdit())
                saveButton.setVisible(true);
                exportButton.setVisible(true);
                break;
            case STUDENT:
                // Ẩn sidebar chọn lớp, hiển thị trực tiếp bảng điểm cá nhân
                splitPane.setLeftComponent(null); // Ẩn sidebar
                splitPane.setDividerSize(0); // Ẩn đường chia
                leftPanel.setVisible(false);
                rightPanel.remove(rightPanel.getComponent(0)); // Xóa label chọn lớp và nút
                // Tải dữ liệu của học sinh hiện tại
                controller.loadDataForCurrentStudent();
                // Ẩn nút Save và Export
                saveButton.setVisible(false);
                exportButton.setVisible(false);
                // Có thể thêm tiêu đề khác cho rightPanel
                rightPanel.setBorder(BorderFactory.createTitledBorder("Bảng điểm cá nhân"));
                break;
            default:
                // Trường hợp không xác định, ẩn hết?
                splitPane.setLeftComponent(null);
                splitPane.setRightComponent(null);
                this.removeAll();
                this.add(new JLabel("Truy cập bị giới hạn.", SwingConstants.CENTER));
                break;
        }
        this.revalidate();
        this.repaint();
    }

    // Tải danh sách lớp vào JList
    private void loadClassList() {
        if(controller != null) {
            classListModel.clear();
            List<EduClass> classes = controller.getClassesForCurrentUser();
            if (classes != null) {
                classes.forEach(classListModel::addElement);
            }
        }
    }

    // --- Cập nhật dữ liệu bảng (Admin/Teacher) ---
    public void updateTableData(List<Student> students, List<AcademicRecord> records) {
        gradeTableModel.setRowCount(0); // Xóa dữ liệu cũ
        if (students == null || records == null || students.size() != records.size()) {
            System.err.println("Error updating table: Mismatch between students and records lists or lists are null.");
            return; // Dữ liệu không khớp, không cập nhật
        }

        for (int i = 0; i < students.size(); i++) {
            Student student = students.get(i);
            AcademicRecord record = records.get(i);
            Vector<Object> row = new Vector<>();

            row.add(i + 1); // STT
            row.add(student.getFullName());
            // Lấy điểm các môn theo key đã thống nhất
            row.add(record.getGrade("Toán"));
            row.add(record.getGrade("Văn"));
            row.add(record.getGrade("Anh"));
            row.add(record.getGrade("Lí"));
            row.add(record.getGrade("Hoá"));
            row.add(record.getGrade("Sinh"));
            row.add(record.getGrade("Sử"));
            row.add(record.getGrade("Địa"));
            row.add(record.getGrade("GDCD"));
            row.add(record.getArtStatus()); // Thêm thẳng Enum
            // Tính toán và thêm điểm TB (format nếu cần)
            row.add(record.calculateAvgNaturalSciences());
            row.add(record.calculateAvgSocialSciences());
            row.add(record.calculateAvgOverallSubjects());
            row.add(record.getConductRating()); // Thêm thẳng Enum

            gradeTableModel.addRow(row);
        }
        saveButton.setEnabled(false); // Reset nút save sau khi tải lại
        hasPendingChanges = false;
        exportButton.setEnabled(gradeTableModel.getRowCount() > 0); // Enable export nếu có data
    }

    // --- Cập nhật bảng điểm cho Student ---
    public void updateTableDataForStudent(Student student, List<AcademicRecord> records) {
        // Sinh viên chỉ xem, không cần sửa, cấu trúc bảng có thể khác
        // Tạm thời vẫn dùng model cũ nhưng không cho sửa
        gradeTableModel.setRowCount(0);
        if (student == null || records == null) return;

        // Có thể hiển thị theo từng kỳ/năm nếu có thông tin đó trong record
        for (AcademicRecord record : records) {
            Vector<Object> row = new Vector<>();
            // Có thể cần thêm cột Kỳ/Năm học ở đây
            row.add(1); // STT luôn là 1
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
        saveButton.setEnabled(false);
        exportButton.setEnabled(false); // Sinh viên không export bảng điểm lớp
    }


    // --- Cập nhật các ô tính toán trên giao diện sau khi sửa điểm ---
    public void updateCalculatedValues(int rowIndex, AcademicRecord record) {
        if (rowIndex >= 0 && rowIndex < gradeTableModel.getRowCount()) {
            gradeTableModel.setValueAt(record.calculateAvgNaturalSciences(), rowIndex, getColumnIndex("TB KHTN"));
            gradeTableModel.setValueAt(record.calculateAvgSocialSciences(), rowIndex, getColumnIndex("TB KHXH"));
            gradeTableModel.setValueAt(record.calculateAvgOverallSubjects(), rowIndex, getColumnIndex("TB môn học"));
        }
    }

    // --- Đánh dấu có thay đổi chưa lưu ---
    public void markChangesPending(boolean pending) {
        this.hasPendingChanges = pending;
        saveButton.setEnabled(pending && controller.canCurrentUserEdit()); // Chỉ bật nếu có quyền sửa
    }

    // --- Refresh lại một ô cụ thể (ví dụ khi validation fail) ---
    public void refreshTableCell(int rowIndex, String subjectKey){
        if(rowIndex >= 0 && rowIndex < gradeTableModel.getRowCount()){
            int columnIndex = getColumnIndex(subjectKey);
            if(columnIndex != -1){
                System.out.println("Requesting repaint for cell: row=" + rowIndex + ", col=" + columnIndex);
                // Chỉ cần yêu cầu model báo cáo rằng ô đã thay đổi để nó được vẽ lại
                gradeTableModel.fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }

    // --- Helper lấy index cột theo tên ---
    private int getColumnIndex(String columnName) {
        for (int i = 0; i < gradeTable.getColumnCount(); i++) {
            if (gradeTable.getColumnName(i).equals(columnName)) {
                return i;
            }
        }
        return -1; // Không tìm thấy
    }


    // Renderer tùy chỉnh cho JList hiển thị tên lớp
    private static class EduClassListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof EduClass) {
                EduClass eduClass = (EduClass) value;
                // Hiển thị tên lớp (có thể thêm thông tin khác nếu muốn)
                label.setText(eduClass.getClassName());
                // Có thể thêm Icon tùy theo trạng thái lớp nếu muốn
            }
            return label;
        }
    }
    public void configureControlsForRole(Role userRole) { // <<< PHẢI CÓ PHƯƠNG THỨC NÀY
        this.currentUserRole = userRole; // Lưu role nếu cần

        System.out.println("EducationPanel: Configuring controls for role: " + userRole); // Thêm log để kiểm tra

        if (controller == null) {
            System.err.println("EducationPanel: Cannot configure controls, controller is null.");
            if(splitPane != null) splitPane.setVisible(false); // Ẩn đi nếu chưa sẵn sàng
            return;
        }

        // --- Logic ẩn/hiện dựa trên role như đã viết trước đó ---
        switch (userRole) {
            case ADMIN:
            case TEACHER:
                if(splitPane != null) {
                    splitPane.setLeftComponent(leftPanel);
                    splitPane.setDividerSize(8);
                    splitPane.setVisible(true);
                }
                if(leftPanel != null) leftPanel.setVisible(true);
                if(rightPanel != null) {
                    Component topComponent = ((BorderLayout)rightPanel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
                    if (topComponent != null) topComponent.setVisible(true);
                    if(saveButton != null) saveButton.setVisible(true);
                    if(exportButton != null) exportButton.setVisible(true);
                }
                loadClassList(); // Tải danh sách lớp
                if(saveButton != null) saveButton.setEnabled(hasPendingChanges && controller.canCurrentUserEdit());
                if(exportButton != null) exportButton.setEnabled(gradeTableModel != null && gradeTableModel.getRowCount() > 0);
                break;

            case STUDENT:
                if(splitPane != null) {
                    splitPane.setLeftComponent(null);
                    splitPane.setDividerSize(0);
                    splitPane.setVisible(true);
                }
                if(leftPanel != null) leftPanel.setVisible(false);
                if(rightPanel != null) {
                    Component topComponent = ((BorderLayout)rightPanel.getLayout()).getLayoutComponent(BorderLayout.NORTH);
                    if (topComponent != null) topComponent.setVisible(false);
                }
                controller.loadDataForCurrentStudent();
                if(saveButton != null) saveButton.setVisible(false);
                if(exportButton != null) exportButton.setVisible(false);
                if (rightPanel != null) rightPanel.setBorder(BorderFactory.createTitledBorder("Bảng điểm cá nhân")); // Có thể đặt title
                break;

            default:
                if(splitPane != null) splitPane.setVisible(false);
                this.removeAll();
                this.add(new JLabel("Access Restricted.", SwingConstants.CENTER));
                break;
        }
        // Yêu cầu vẽ lại
        this.revalidate();
        this.repaint();
    }

}