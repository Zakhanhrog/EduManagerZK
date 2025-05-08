package com.eduzk.view.panels;

import com.eduzk.controller.UserController;
import com.eduzk.model.entities.Role;
import com.eduzk.model.entities.User;
import com.eduzk.utils.UIUtils;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.Vector;
import javax.swing.Icon;
import java.net.URL;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import com.eduzk.utils.ValidationUtils;
import com.formdev.flatlaf.extras.FlatSVGIcon;

public class AccountsPanel extends JPanel {
    private UserController controller;
    private JTable accountsTable;
    private DefaultTableModel tableModel;
    private JButton editPasswordButton;
    private JButton refreshButton;
    private TableRowSorter<DefaultTableModel> sorter;
    private JRadioButton showTeachersRadio;
    private JRadioButton showStudentsRadio;
    private ButtonGroup roleFilterGroup;
    private List<User> allUsersCache = new ArrayList<>();

    public AccountsPanel(UserController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
        setupLayout();
        setupActions();
    }

    public void setController(UserController controller) {
        this.controller = controller;
        if (this.isShowing()) {
            refreshTable();
        }
    }

    private void initComponents() {
        // Table Model
        tableModel = new DefaultTableModel(
                new Object[]{"STT", "Username", "Password", "Role", "Active", "UserID"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 4) return Boolean.class;
                return super.getColumnClass(columnIndex);
            }
        };
        accountsTable = new JTable(tableModel);
        accountsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accountsTable.setAutoCreateRowSorter(true);
        sorter = (TableRowSorter<DefaultTableModel>) accountsTable.getRowSorter();

        TableColumn userIdColumn = accountsTable.getColumnModel().getColumn(5);
        userIdColumn.setMinWidth(0);
        userIdColumn.setMaxWidth(0);
        userIdColumn.setPreferredWidth(0);
        userIdColumn.setResizable(false);

        // Buttons
        int iconSize = 20;
        editPasswordButton = new JButton("Edit Password");
        Icon editPassIcon = loadSVGIconButton("/icons/edit.svg", iconSize);
        if(editPassIcon!=null) editPasswordButton.setIcon(editPassIcon);
        editPasswordButton.setToolTipText("Change password for the selected user");

        refreshButton = new JButton("Refresh");
        Icon refreshIcon = loadSVGIconButton("/icons/refresh.svg", iconSize);
        if (refreshIcon != null) refreshButton.setIcon(refreshIcon);
        refreshButton.setToolTipText("Reload user account list");

        showTeachersRadio = new JRadioButton("Teacher Accounts", true);
        showStudentsRadio = new JRadioButton("Student Accounts");
        roleFilterGroup = new ButtonGroup();
        roleFilterGroup.add(showTeachersRadio);
        roleFilterGroup.add(showStudentsRadio);

    }

    private void setupLayout() {
        // --- Top Panel ---
        JPanel topPanel = new JPanel(new BorderLayout(20, 0));

        // --- Left Panel (Filter Radios) ---
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(showTeachersRadio);
        filterPanel.add(showStudentsRadio);

        // --- Right Panel (Action Buttons) ---
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.add(refreshButton);
        actionPanel.add(editPasswordButton);

        topPanel.add(filterPanel, BorderLayout.WEST);
        topPanel.add(actionPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center Panel (Table)
        JScrollPane scrollPane = new JScrollPane(accountsTable);
        add(scrollPane, BorderLayout.CENTER);

        // Điều chỉnh độ rộng cột (tùy chọn)
        TableColumn sttCol = accountsTable.getColumnModel().getColumn(0);
        sttCol.setPreferredWidth(40); sttCol.setMaxWidth(60);
        TableColumn userCol = accountsTable.getColumnModel().getColumn(1);
        userCol.setPreferredWidth(200);
        TableColumn passCol = accountsTable.getColumnModel().getColumn(2);
        passCol.setPreferredWidth(120);
        TableColumn roleCol = accountsTable.getColumnModel().getColumn(3);
        roleCol.setPreferredWidth(100);
        TableColumn activeCol = accountsTable.getColumnModel().getColumn(4);
        activeCol.setPreferredWidth(60); activeCol.setMaxWidth(80);
    }

    private void setupActions() {
        refreshButton.addActionListener(e -> refreshTable());
        editPasswordButton.addActionListener(e -> openEditPasswordDialog());
        ActionListener roleFilterListener = e -> filterTable();
        showTeachersRadio.addActionListener(roleFilterListener);
        showStudentsRadio.addActionListener(roleFilterListener);

    }

    private void filterTable() {
        Role selectedRole = showTeachersRadio.isSelected() ? Role.TEACHER : Role.STUDENT;
        System.out.println("AccountsPanel: Filtering table for role: " + selectedRole);
        populateTable(this.allUsersCache, selectedRole);
    }

    private void openEditPasswordDialog() {
        int selectedViewRow = accountsTable.getSelectedRow();
        if (selectedViewRow < 0) {
            UIUtils.showWarningMessage(this, "Selection Required", "Please select a user account to edit the password.");
            return;
        }
        if (controller == null) {
            UIUtils.showErrorMessage(this, "Error", "User Controller is not available.");
            return;
        }

        try {
            int modelRow = accountsTable.convertRowIndexToModel(selectedViewRow);
            int userId = (int) tableModel.getValueAt(modelRow, 5);
            String username = (String) tableModel.getValueAt(modelRow, 1);

            JPasswordField passwordField = new JPasswordField(20);
            JPanel panel = new JPanel(new GridLayout(2, 1));
            panel.add(new JLabel("Enter new password for user: " + username));
            panel.add(passwordField);

            int option = JOptionPane.showConfirmDialog(this, panel, "Change Password",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (option == JOptionPane.OK_OPTION) {
                String newPassword = new String(passwordField.getPassword());
                if (ValidationUtils.isValidPassword(newPassword)) {
                    controller.updateUserPassword(userId, newPassword);
                } else {
                    UIUtils.showWarningMessage(this, "Validation Error", "Password must be at least 6 characters long.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error preparing password change: " + e.getMessage());
            UIUtils.showErrorMessage(this, "Error", "Could not get user information for password change.");
        }
    }

    public void refreshTable() {
        if (controller == null) {
            System.err.println("AccountsPanel: Cannot refresh, UserController is null.");
            tableModel.setRowCount(0);
            this.allUsersCache.clear();
            return;
        }
        System.out.println("AccountsPanel: Refreshing table data using UserController...");
        this.allUsersCache = controller.getAllUserAccounts();
        System.out.println("AccountsPanel: Fetched " + (this.allUsersCache == null ? "null" : this.allUsersCache.size()) + " user accounts from UserController."); // Log kích thước cache
        filterTable();
    }

    private void populateTable(List<User> users, Role roleToDisplay) {
        tableModel.setRowCount(0);
        int stt = 1;
        if (users != null) {
            for (User user : users) {
                if (user.getRole() == roleToDisplay) {
                    Vector<Object> row = new Vector<>();
                    row.add(stt++);
                    row.add(user.getUsername());
                    row.add(user.getPassword());
                    row.add(user.getRole().getDisplayName());
                    row.add(user.isActive());
                    row.add(user.getUserId());
                    tableModel.addRow(row);
                }
            }
        }
        System.out.println("AccountsPanel: Table populated with " + tableModel.getRowCount() + " rows for role " + roleToDisplay);
    }

    public void configureControlsForRole(Role userRole) {
        boolean canManageAccounts = (userRole == Role.ADMIN);
        if (editPasswordButton != null) editPasswordButton.setVisible(canManageAccounts);
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