package com.eduzk.view.panels;

import com.eduzk.controller.LogController;
import com.eduzk.model.entities.LogEntry;
import com.eduzk.model.entities.Role;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.Vector;

public class LogsPanel extends JPanel {

    private LogController controller;
    private JTable logTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    public LogsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
        setupLayout();
    }

    public void setController(LogController controller) {
        this.controller = controller;
        if (this.controller != null) {
            this.controller.setLogsPanel(this);
            System.out.println("LogsPanel: Controller has been set.");
        } else {
            tableModel.setRowCount(0);
            System.out.println("LogsPanel: Controller set to null, table cleared.");
        }
    }

    private void initComponents() {
        tableModel = new DefaultTableModel(
                new Object[]{"Timestamp", "User", "Role", "Action", "Details"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Log không cho sửa
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return String.class;
                }
                return super.getColumnClass(columnIndex);
            }
        };
        logTable = new JTable(tableModel);
        logTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        logTable.setAutoCreateRowSorter(true); // Cho phép sort
        sorter = (TableRowSorter<DefaultTableModel>) logTable.getRowSorter();
    }

    private void setupLayout() {
        JScrollPane scrollPane = new JScrollPane(logTable);
        add(scrollPane, BorderLayout.CENTER);

        // Điều chỉnh cột (giữ nguyên)
        logTable.getColumnModel().getColumn(0).setPreferredWidth(140); // Timestamp
        logTable.getColumnModel().getColumn(1).setPreferredWidth(120); // User
        logTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Role
        logTable.getColumnModel().getColumn(3).setPreferredWidth(150); // Action
        logTable.getColumnModel().getColumn(4).setPreferredWidth(350); // Details
    }

    public void refreshTable() {
        if (controller == null) {
            // Chỉ log lỗi, không cần hiển thị message vì người dùng không trực tiếp gây ra
            System.err.println("LogsPanel: Cannot refresh, controller is null.");
            tableModel.setRowCount(0); // Xóa bảng
            return;
        }
        System.out.println("LogsPanel: Refreshing table data requested by controller...");
        // Lấy dữ liệu mới nhất từ controller
        List<LogEntry> logs = controller.getAllLogsForDisplay();
        // Cập nhật bảng với dữ liệu mới
        populateTable(logs);
    }

    private void populateTable(List<LogEntry> logs) {
        // <<< LƯU TRẠNG THÁI SORT HIỆN TẠI >>>
        List<? extends RowSorter.SortKey> sortKeys = null;
        if (sorter != null) {
            sortKeys = sorter.getSortKeys();
        }

        tableModel.setRowCount(0); // Xóa dữ liệu cũ trước khi thêm mới

        if (logs != null && !logs.isEmpty()) {
            System.out.println("LogsPanel: Populating table with " + logs.size() + " entries.");
            for (LogEntry entry : logs) {
                // <<< Đảm bảo LogEntry có các phương thức getter này >>>
                // <<< Và getFormattedTimestamp trả về String >>>
                Vector<Object> row = new Vector<>();
                row.add(entry.getFormattedTimestamp()); // Format cụ thể
                row.add(entry.getUsername());
                row.add(entry.getUserRole());
                row.add(entry.getAction());
                row.add(entry.getDetails());
                tableModel.addRow(row);
            }
        } else {
            System.out.println("LogsPanel: No log entries to populate.");
        }

        if (sorter != null && sortKeys != null && !sortKeys.isEmpty()) {
            System.out.println("LogsPanel: Restoring previous sort keys.");
            sorter.setSortKeys(sortKeys);
        } else {
            System.out.println("LogsPanel: No previous sort keys to restore or sorter is null.");
        }
        System.out.println("LogsPanel: Table population complete.");
    }

    public void configureControlsForRole(Role userRole) {
        boolean isAdmin = (userRole == Role.ADMIN);
        if (isAdmin && controller != null) {
            System.out.println("LogsPanel: Configured for Admin, requesting initial data load via controller.");
            controller.requestPanelRefresh();
        } else if (!isAdmin) {
            tableModel.setRowCount(0);
        }
    }
}