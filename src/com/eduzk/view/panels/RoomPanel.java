package com.eduzk.view.panels;

import com.eduzk.controller.RoomController;
import com.eduzk.model.entities.Room;
import com.eduzk.utils.UIUtils;
import com.eduzk.view.dialogs.RoomDialog;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.Vector;
import javax.swing.Icon;
import java.net.URL;
import com.formdev.flatlaf.extras.FlatSVGIcon;

public class RoomPanel extends JPanel {

    private RoomController controller;
    private JTable roomTable;
    private DefaultTableModel tableModel;
    private JButton addButton, editButton, deleteButton, searchButton;
    private JButton refreshButton;
    private JTextField searchField; // Search by capacity
    private TableRowSorter<DefaultTableModel> sorter;

    public RoomPanel(RoomController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
        setupLayout();
        setupActions();
    }

    public void setController(RoomController controller) {
        this.controller = controller;
        refreshTable(); // Initial load
    }

    private void initComponents() {
        // Table Model
        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Number", "Building", "Capacity", "Type", "Available"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 5) return Boolean.class; // 'Available' column
                if (columnIndex == 3) return Integer.class; // 'Capacity' column
                return super.getColumnClass(columnIndex);
            }
        };
        roomTable = new JTable(tableModel);
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomTable.setAutoCreateRowSorter(true);
        sorter = (TableRowSorter<DefaultTableModel>) roomTable.getRowSorter();

        // Adjust column widths
        TableColumn idCol = roomTable.getColumnModel().getColumn(0);
        idCol.setPreferredWidth(40);
        idCol.setMaxWidth(60);
        TableColumn numCol = roomTable.getColumnModel().getColumn(1);
        numCol.setPreferredWidth(100);
        TableColumn buildCol = roomTable.getColumnModel().getColumn(2);
        buildCol.setPreferredWidth(120);
        TableColumn capCol = roomTable.getColumnModel().getColumn(3);
        capCol.setPreferredWidth(70);
        TableColumn typeCol = roomTable.getColumnModel().getColumn(4);
        typeCol.setPreferredWidth(100);
        TableColumn availCol = roomTable.getColumnModel().getColumn(5);
        availCol.setPreferredWidth(70);


        // Buttons
        int iconSize = 16;

        addButton = new JButton("Add"); // Giả sử tên nút
        Icon addIcon = loadSVGIconButton("/icons/add.svg", iconSize);
        if (addIcon != null) addButton.setIcon(addIcon);

        editButton = new JButton("Edit");
        Icon editIcon = loadSVGIconButton("/icons/edit.svg", iconSize);
        if (editIcon != null) editButton.setIcon(editIcon);

        deleteButton = new JButton("Delete");
        Icon deleteIcon = loadSVGIconButton("/icons/delete.svg", iconSize);
        if (deleteIcon != null) deleteButton.setIcon(deleteIcon);

        refreshButton = new JButton("Refresh");
        Icon refreshIcon = loadSVGIconButton("/icons/refresh.svg", iconSize);
        if (refreshIcon != null) refreshButton.setIcon(refreshIcon);


        // Search Components
        searchField = new JTextField(5); // Small field for capacity number
        searchButton = new JButton("Find Capacity >=");
    }

    private void setupLayout() {
        // Top Panel (Search and Actions)
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Min Capacity:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.add(refreshButton);
        actionPanel.add(addButton);
        actionPanel.add(editButton);
        actionPanel.add(deleteButton);

        topPanel.add(searchPanel, BorderLayout.WEST);
        topPanel.add(actionPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center Panel (Table)
        JScrollPane scrollPane = new JScrollPane(roomTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void setupActions() {
        addButton.addActionListener(e -> openRoomDialog(null));

        editButton.addActionListener(e -> {
            int selectedRow = roomTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = roomTable.convertRowIndexToModel(selectedRow);
                int roomId = (int) tableModel.getValueAt(modelRow, 0);
                Room roomToEdit = controller.getRoomById(roomId);
                if (roomToEdit != null) {
                    openRoomDialog(roomToEdit);
                } else {
                    UIUtils.showErrorMessage(this, "Error", "Could not retrieve room details for editing.");
                }
            } else {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select a room to edit.");
            }
        });

        deleteButton.addActionListener(e -> {
            int selectedRow = roomTable.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = roomTable.convertRowIndexToModel(selectedRow);
                int roomId = (int) tableModel.getValueAt(modelRow, 0);
                String roomNumber = (String) tableModel.getValueAt(modelRow, 1);

                if (UIUtils.showConfirmDialog(this, "Confirm Deletion", "Are you sure you want to delete room '" + roomNumber + "' (ID: " + roomId + ")?")) {
                    if (controller != null) {
                        controller.deleteRoom(roomId);
                    }
                }
            } else {
                UIUtils.showWarningMessage(this, "Selection Required", "Please select a room to delete.");
            }
        });

        searchButton.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());
        refreshButton.addActionListener(e -> {
            System.out.println("RoomPanel: Refresh button clicked.");
            refreshTable(); // Gọi lại chính phương thức refresh của panel này
            UIUtils.showInfoMessage(this,"Refreshed", "Room list updated."); // Thông báo (tùy chọn)
        });
    }

    private void performSearch() {
        if (controller == null) return;
        String searchText = searchField.getText().trim();
        List<Room> rooms;

        if (searchText.isEmpty()) {
            // If capacity search is empty, show all
            rooms = controller.getAllRooms();
            sorter.setRowFilter(null); // Clear any filter
        } else {
            try {
                int minCapacity = Integer.parseInt(searchText);
                if (minCapacity <= 0) {
                    // Show all if capacity is not positive
                    rooms = controller.getAllRooms();
                    sorter.setRowFilter(null);
                    if (!searchText.equals("0")) { // Only warn if not exactly "0"
                        UIUtils.showWarningMessage(this, "Invalid Input", "Minimum capacity must be a positive number.");
                    }
                    searchField.setText(""); // Clear invalid input
                } else {
                    // Search via controller DAO method
                    rooms = controller.searchRoomsByCapacity(minCapacity);
                    // No RowFilter needed if DAO does the search
                }
            } catch (NumberFormatException ex) {
                UIUtils.showErrorMessage(this, "Invalid Input", "Please enter a valid number for minimum capacity.");
                rooms = controller.getAllRooms(); // Show all on error
                sorter.setRowFilter(null);
                searchField.setText(""); // Clear invalid input
            }
        }
        populateTable(rooms);
    }


    private void openRoomDialog(Room room) {
        if (controller == null) {
            UIUtils.showErrorMessage(this, "Error", "Room Controller is not initialized.");
            return;
        }
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        RoomDialog dialog = new RoomDialog((Frame) parentWindow, controller, room);
        dialog.setVisible(true);
    }

    public void refreshTable() {
        if (controller == null) return;
        List<Room> rooms = controller.getAllRooms();
        populateTable(rooms);
    }

    private void populateTable(List<Room> rooms) {
        tableModel.setRowCount(0); // Clear existing data
        if (rooms != null) {
            for (Room room : rooms) {
                Vector<Object> row = new Vector<>();
                row.add(room.getRoomId());
                row.add(room.getRoomNumber());
                row.add(room.getBuilding());
                row.add(room.getCapacity());
                row.add(room.getType());
                row.add(room.isAvailable());
                tableModel.addRow(row);
            }
        }
    }

    public void setAdminControlsEnabled(boolean isAdmin) {
        addButton.setVisible(isAdmin); // Hoặc setEnabled(isAdmin)
        editButton.setVisible(isAdmin);
        deleteButton.setVisible(isAdmin);
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