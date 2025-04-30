package com.eduzk.view.dialogs;

import com.eduzk.controller.RoomController;
import com.eduzk.model.entities.Room;
import com.eduzk.utils.UIUtils;
import com.eduzk.utils.ValidationUtils;

import javax.swing.*;
import java.awt.*;

public class RoomDialog extends JDialog {

    private final RoomController controller;
    private final Room roomToEdit;
    private final boolean isEditMode;

    // UI Components
    private JTextField idField;
    private JTextField numberField;
    private JTextField buildingField;
    private JSpinner capacitySpinner;
    private JTextField typeField;
    private JCheckBox availableCheckBox;
    private JButton saveButton;
    private JButton cancelButton;

    public RoomDialog(Frame owner, RoomController controller, Room room) {
        super(owner, true);
        this.controller = controller;
        this.roomToEdit = room;
        this.isEditMode = (room != null);

        setTitle(isEditMode ? "Edit Room" : "Add Room");
        initComponents();
        setupLayout();
        setupActions();
        populateFields();
        configureDialog();
    }

    private void initComponents() {
        idField = new JTextField(5);
        idField.setEditable(false);
        numberField = new JTextField(15);
        buildingField = new JTextField(20);
        capacitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1)); // Min capacity 1
        typeField = new JTextField(15); // E.g., Classroom, Lab
        availableCheckBox = new JCheckBox("Available");
        availableCheckBox.setSelected(true); // Default available

        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
    }

    private void setupLayout() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        int currentRow = 0;

        // Row: ID (Edit mode only)
        if (isEditMode) {
            gbc.gridx = 0; gbc.gridy = currentRow;
            formPanel.add(new JLabel("ID:"), gbc);
            gbc.gridx = 1; gbc.gridy = currentRow; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
            formPanel.add(idField, gbc);
            currentRow++;
        }

        // Row: Number & Building
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Number*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.4;
        formPanel.add(numberField, gbc);

        gbc.gridx = 2; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Building:"), gbc);
        gbc.gridx = 3; gbc.gridy = currentRow; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.6;
        formPanel.add(buildingField, gbc);
        currentRow++;

        // Row: Capacity & Type
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("Capacity*:"), gbc);
        gbc.gridx = 1; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.4;
        formPanel.add(capacitySpinner, gbc);

        gbc.gridx = 2; gbc.gridy = currentRow; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 3; gbc.gridy = currentRow; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.6;
        formPanel.add(typeField, gbc);
        currentRow++;

        // Row: Available Status
        gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        formPanel.add(availableCheckBox, gbc);
        currentRow++;


        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        // Add panels to dialog
        setLayout(new BorderLayout());
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupActions() {
        saveButton.addActionListener(e -> saveRoom());
        cancelButton.addActionListener(e -> dispose());
    }

    private void populateFields() {
        if (isEditMode && roomToEdit != null) {
            idField.setText(String.valueOf(roomToEdit.getRoomId()));
            numberField.setText(roomToEdit.getRoomNumber());
            buildingField.setText(roomToEdit.getBuilding());
            capacitySpinner.setValue(roomToEdit.getCapacity());
            typeField.setText(roomToEdit.getType());
            availableCheckBox.setSelected(roomToEdit.isAvailable());
        } else {
            capacitySpinner.setValue(1); // Default capacity
            availableCheckBox.setSelected(true); // Default available
        }
    }

    private void configureDialog() {
        pack();
        setMinimumSize(new Dimension(400, 250));
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }


    private void saveRoom() {
        // --- Input Validation ---
        String number = numberField.getText().trim();
        String building = buildingField.getText().trim();
        int capacity = (int) capacitySpinner.getValue();
        String type = typeField.getText().trim();
        boolean isAvailable = availableCheckBox.isSelected();


        if (!ValidationUtils.isNotEmpty(number)) {
            UIUtils.showWarningMessage(this, "Validation Error", "Room Number cannot be empty.");
            numberField.requestFocusInWindow();
            return;
        }
        if (capacity <= 0) { // Spinner model ensures >= 1, but good practice
            UIUtils.showWarningMessage(this, "Validation Error", "Capacity must be positive.");
            capacitySpinner.requestFocusInWindow();
            return;
        }


        // --- Create or Update Room Object ---
        Room room = isEditMode ? roomToEdit : new Room();
        room.setRoomNumber(number);
        room.setBuilding(building);
        room.setCapacity(capacity);
        room.setType(type);
        room.setAvailable(isAvailable);
        // ID handled by DAO or exists already

        // --- Call Controller ---
        boolean success;
        if (isEditMode) {
            success = controller.updateRoom(room);
        } else {
            success = controller.addRoom(room);
        }

        if (success) {
            dispose(); // Close dialog on success
        }
        // Controller shows messages
    }
}