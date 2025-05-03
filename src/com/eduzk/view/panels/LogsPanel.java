package com.eduzk.view.panels;

import com.eduzk.controller.LogController;
import com.eduzk.model.entities.LogEntry;
import com.eduzk.model.entities.Role;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import javax.swing.Icon;
import java.net.URL;
import com.formdev.flatlaf.extras.FlatSVGIcon;

public class LogsPanel extends JPanel {

    private LogController controller;
    private JTable logTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private TableRowSorter<DefaultTableModel> sorter;

    public LogsPanel(LogController controller) {
        this.controller = controller; // Có thể null ban đầu
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
        setupLayout();
        setupActions();
    }

    public void setController(LogController controller) {
        this.controller = controller;
        // Tải dữ liệu khi controller được set VÀ panel đang hiển thị
        if (this.isShowing() && controller != null) {
            refreshTable();
        } else if (controller == null) {
            tableModel.setRowCount(0); // Xóa bảng nếu controller bị null
        }
    }

    private void initComponents() {
        // Table Model
        tableModel = new DefaultTableModel(
                new Object[]{"Timestamp", "User", "Role", "Action", "Details"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Log không cho sửa
            }
        };
        logTable = new JTable(tableModel);
        logTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        logTable.setAutoCreateRowSorter(true); // Cho phép sort
        sorter = (TableRowSorter<DefaultTableModel>) logTable.getRowSorter();

        // Buttons
        int iconSize = 16;
        refreshButton = new JButton("Refresh");
        Icon refreshIcon = loadSVGIconButton("/icons/refresh.svg", iconSize);
        if (refreshIcon != null) refreshButton.setIcon(refreshIcon);
        refreshButton.setToolTipText("Reload log entries");
    }

    private void setupLayout() {
        // Top Panel chỉ chứa nút Refresh
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(refreshButton);
        add(topPanel, BorderLayout.NORTH);

        // Center Panel (Table)
        JScrollPane scrollPane = new JScrollPane(logTable);
        add(scrollPane, BorderLayout.CENTER);

        // Điều chỉnh cột
        logTable.getColumnModel().getColumn(0).setPreferredWidth(140); // Timestamp
        logTable.getColumnModel().getColumn(1).setPreferredWidth(120); // User
        logTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Role
        logTable.getColumnModel().getColumn(3).setPreferredWidth(150); // Action
        logTable.getColumnModel().getColumn(4).setPreferredWidth(350); // Details
    }

    private void setupActions() {
        refreshButton.addActionListener(e -> refreshTable());
    }

    /**
     * Làm mới bảng log bằng cách lấy dữ liệu từ controller.
     */
    public void refreshTable() {
        if (controller == null) {
            System.err.println("LogsPanel: Cannot refresh, controller is null.");
            tableModel.setRowCount(0);
            return;
        }
        System.out.println("LogsPanel: Refreshing table...");
        // Gọi controller để lấy log (controller sẽ kiểm tra quyền)
        List<LogEntry> logs = controller.getAllLogsForDisplay();
        populateTable(logs);
    }

    private void populateTable(List<LogEntry> logs) {
        tableModel.setRowCount(0); // Xóa dữ liệu cũ
        if (logs != null) {
            for (LogEntry entry : logs) {
                Vector<Object> row = new Vector<>();
                row.add(entry.getFormattedTimestamp()); // Dùng timestamp đã định dạng
                row.add(entry.getUsername());
                row.add(entry.getUserRole());
                row.add(entry.getAction());
                row.add(entry.getDetails());
                tableModel.addRow(row);
            }
        }
        System.out.println("LogsPanel: Table populated with " + tableModel.getRowCount() + " log entries.");
    }

    /**
     * Cấu hình control (hiện tại chỉ ẩn/hiện toàn bộ panel dựa trên quyền Admin).
     * @param userRole Vai trò người dùng.
     */
    public void configureControlsForRole(Role userRole) {
        boolean isAdmin = (userRole == Role.ADMIN);
        // Nếu không phải Admin, có thể xóa hết nội dung hoặc không làm gì cả
        // vì tab này chỉ được thêm cho Admin trong MainView
        if (!isAdmin) {
            tableModel.setRowCount(0); // Xóa dữ liệu nếu không phải admin (cho chắc)
            // Có thể ẩn các nút nếu có nhiều nút hơn
            if (refreshButton != null) refreshButton.setVisible(false);
        } else {
            if (refreshButton != null) refreshButton.setVisible(true);
            // Gọi refresh lần đầu khi panel được cấu hình cho Admin
            refreshTable();
        }
    }

    // Hàm helper load SVG icon (Copy từ panel khác)
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